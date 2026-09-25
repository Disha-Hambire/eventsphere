package com.eventsphere.service;

import com.eventsphere.dto.RegistrationDtos.RegistrationDto;
import com.eventsphere.dto.RegistrationDtos.RegistrationResult;
import com.eventsphere.dto.RegistrationDtos.TimelineEntry;
import com.eventsphere.entity.*;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ConflictException;
import com.eventsphere.exception.ForbiddenException;
import com.eventsphere.exception.NotFoundException;
import com.eventsphere.repository.AttendanceRepository;
import com.eventsphere.repository.EventRepository;
import com.eventsphere.repository.FeedbackRepository;
import com.eventsphere.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Smart registration (R12-R16): capacity, FIFO waitlist, automatic promotion, duplicate and
 * schedule-clash prevention. Seat changes happen under a row lock on the event.
 */
@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final WaitlistService waitlistService;
    private final TicketCodeGenerator ticketCodes;
    private final AccessPolicy access;
    private final Clock clock;

    public RegistrationService(RegistrationRepository registrationRepository, EventRepository eventRepository,
                               AttendanceRepository attendanceRepository, FeedbackRepository feedbackRepository,
                               WaitlistService waitlistService, TicketCodeGenerator ticketCodes,
                               AccessPolicy access, Clock clock) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.attendanceRepository = attendanceRepository;
        this.feedbackRepository = feedbackRepository;
        this.waitlistService = waitlistService;
        this.ticketCodes = ticketCodes;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public RegistrationResult register(Long eventId, User participant) {
        LocalDateTime now = now();
        if (!access.isParticipant(participant)) {
            throw new ForbiddenException("Only participants can register for events");
        }
        // Lock the event row: concurrent registrations for the last seat are serialised here.
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        // R12
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Registration is not open: this event is " + event.getStatus().name().toLowerCase());
        }
        if (!now.isBefore(event.getRegistrationDeadline())) {
            throw new BusinessRuleException("Registration closed on " + Fmt.dateTime(event.getRegistrationDeadline()));
        }
        // R13
        Registration existing = registrationRepository.findByEventIdAndParticipantId(eventId, participant.getId()).orElse(null);
        if (existing != null && existing.getStatus() != RegistrationStatus.CANCELLED) {
            throw new ConflictException(existing.getStatus() == RegistrationStatus.CONFIRMED
                    ? "You already have a confirmed seat for this event"
                    : "You are already on the waitlist for this event");
        }
        // Smart check: don't let a participant hold seats at two events running at the same time
        List<Registration> overlapping = registrationRepository.findOverlappingConfirmed(
                participant.getId(), eventId, event.getStartDateTime(), event.getEndDateTime());
        if (!overlapping.isEmpty()) {
            Event other = overlapping.get(0).getEvent();
            throw new BusinessRuleException("You already have a seat at '" + other.getTitle() + "' ("
                    + Fmt.dateTime(other.getStartDateTime()) + "), which overlaps with this event. Cancel it first.");
        }

        // R14
        long confirmed = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        boolean seatAvailable = confirmed < event.getCapacity();

        Registration registration;
        if (existing != null) {
            registration = existing;
            if (seatAvailable) {
                registration.rejoinConfirmed(now);
            } else {
                registration.rejoinWaitlist(now);
            }
        } else {
            registration = registrationRepository.save(new Registration(event, participant,
                    seatAvailable ? RegistrationStatus.CONFIRMED : RegistrationStatus.WAITLISTED,
                    ticketCodes.newCode(), now));
        }
        registrationRepository.flush();

        RegistrationDto dto = toDto(registration, participant, now);
        String message = seatAvailable
                ? "You're in! Your seat is confirmed and your QR ticket is ready."
                : "The event is full, so you're #" + dto.waitlistPosition()
                  + " on the waitlist. You'll be confirmed automatically if a seat frees up.";
        return new RegistrationResult(dto, message, null);
    }

    /** R16: cancel before the event starts; a freed seat goes to the first person on the waitlist. */
    @Transactional
    public RegistrationResult cancel(Long registrationId, User user) {
        LocalDateTime now = now();
        Registration registration = find(registrationId);
        Event event = eventRepository.findByIdForUpdate(registration.getEvent().getId()).orElseThrow();
        boolean isOwner = registration.getParticipant().getId().equals(user.getId());
        if (!isOwner && !access.canManage(event, user)) {
            throw new ForbiddenException("You can only cancel your own registration");
        }
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new ConflictException("This registration is already cancelled");
        }
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("This event is " + event.getStatus().name().toLowerCase()
                    + "; registrations can no longer be changed");
        }
        if (event.hasStarted(now)) {
            throw new BusinessRuleException("The event has already started, so the registration can no longer be cancelled");
        }

        boolean wasConfirmed = registration.getStatus() == RegistrationStatus.CONFIRMED;
        registration.cancel(now);
        registrationRepository.flush();

        RegistrationDto promotedDto = null;
        String message = "Your registration has been cancelled.";
        if (wasConfirmed) {
            List<Registration> promoted = waitlistService.fillOpenSeats(event, now);
            if (!promoted.isEmpty()) {
                registrationRepository.flush();
                Registration p = promoted.get(0);
                promotedDto = toDto(p, user, now);
                message += " The seat was automatically given to " + p.getParticipant().getFullName()
                        + " from the waitlist.";
            }
        }
        return new RegistrationResult(toDto(registration, user, now), message, promotedDto);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDto> myRegistrations(User participant) {
        LocalDateTime now = now();
        return registrationRepository.findByParticipantIdOrderByRegisteredAtDesc(participant.getId()).stream()
                .map(r -> toDto(r, participant, now))
                .sorted(Comparator.comparing(RegistrationDto::eventStart).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public RegistrationDto get(Long registrationId, User user) {
        Registration r = find(registrationId);
        boolean isOwner = r.getParticipant().getId().equals(user.getId());
        if (!isOwner && !access.canManage(r.getEvent(), user)) {
            throw new ForbiddenException("You can only view your own registrations");
        }
        return toDto(r, user, now());
    }

    @Transactional(readOnly = true)
    public List<RegistrationDto> forEvent(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event", eventId));
        access.requireManage(event, user);
        LocalDateTime now = now();
        return registrationRepository.findByEventIdOrderByRegisteredAtAsc(eventId).stream()
                .map(r -> toDto(r, user, now))
                .toList();
    }

    /** CSV export of the registration list (opens in Excel). */
    @Transactional(readOnly = true)
    public String exportCsv(Long eventId, User user) {
        List<RegistrationDto> rows = forEvent(eventId, user);
        StringBuilder sb = new StringBuilder("Name,Email,Status,Waitlist Position,Ticket Code,Registered At,Promoted From Waitlist,Sessions Attended,Feedback Given\n");
        for (RegistrationDto r : rows) {
            sb.append(csv(r.participantName())).append(',')
                    .append(csv(r.participantEmail())).append(',')
                    .append(r.status()).append(',')
                    .append(r.waitlistPosition() == null ? "" : r.waitlistPosition()).append(',')
                    .append(r.ticketCode() == null ? "" : r.ticketCode()).append(',')
                    .append(csv(Fmt.dateTime(r.registeredAt()))).append(',') // contains a comma, so quote it
                    .append(r.promotedFromWaitlist() ? "Yes" : "No").append(',')
                    .append(r.sessionsAttended()).append(',')
                    .append(r.feedbackGiven() ? "Yes" : "No").append('\n');
        }
        return sb.toString();
    }

    Registration find(Long id) {
        return registrationRepository.findById(id).orElseThrow(() -> new NotFoundException("Registration", id));
    }

    RegistrationDto toDto(Registration r, User viewer, LocalDateTime now) {
        Event e = r.getEvent();
        User p = r.getParticipant();
        boolean isOwner = viewer != null && p.getId().equals(viewer.getId());
        boolean canSeeTicket = r.getStatus() == RegistrationStatus.CONFIRMED && (isOwner || access.canManage(e, viewer));
        List<Attendance> attendance = attendanceRepository.findByRegistrationId(r.getId());
        Feedback feedback = feedbackRepository.findByEventIdAndParticipantId(e.getId(), p.getId()).orElse(null);
        boolean canCancel = isOwner && r.getStatus() != RegistrationStatus.CANCELLED
                && e.getStatus() == EventStatus.PUBLISHED && !e.hasStarted(now);
        boolean canGiveFeedback = isOwner && e.getStatus() == EventStatus.COMPLETED
                && r.getStatus() == RegistrationStatus.CONFIRMED && !attendance.isEmpty() && feedback == null;

        return new RegistrationDto(
                r.getId(), e.getId(), e.getTitle(), e.getCategory(), e.getVenue(),
                e.getStartDateTime(), e.getEndDateTime(), e.getStatus(),
                p.getId(), p.getFullName(), p.getEmail(),
                r.getStatus(), canSeeTicket ? r.getTicketCode() : null,
                r.getRegisteredAt(), r.getConfirmedAt(), r.getCancelledAt(), r.isPromotedFromWaitlist(),
                waitlistService.waitlistPosition(r), attendance.size(), feedback != null,
                canCancel, canGiveFeedback, timeline(r, attendance, feedback));
    }

    /** The story of one registration, used for the timeline UI. */
    private List<TimelineEntry> timeline(Registration r, List<Attendance> attendance, Feedback feedback) {
        List<TimelineEntry> entries = new ArrayList<>();
        if (r.isPromotedFromWaitlist()) {
            entries.add(new TimelineEntry(r.getRegisteredAt(), "Joined the waitlist (event was full)", "WAITLISTED"));
            entries.add(new TimelineEntry(r.getConfirmedAt(), "Promoted from waitlist - seat confirmed", "PROMOTED"));
        } else if (r.getConfirmedAt() != null) {
            entries.add(new TimelineEntry(r.getRegisteredAt(), "Registered - seat confirmed", "CONFIRMED"));
        } else if (r.getStatus() == RegistrationStatus.CANCELLED) {
            entries.add(new TimelineEntry(r.getRegisteredAt(), "Registered", "REGISTERED"));
        } else {
            entries.add(new TimelineEntry(r.getRegisteredAt(), "Joined the waitlist (event was full)", "WAITLISTED"));
        }
        for (Attendance a : attendance) {
            entries.add(new TimelineEntry(a.getCheckedInAt(),
                    "Checked in: " + a.getSession().getTitle() + (a.getMethod() == Attendance.Method.QR ? " (QR scan)" : " (manual)"),
                    "CHECKED_IN"));
        }
        if (r.getCancelledAt() != null) {
            entries.add(new TimelineEntry(r.getCancelledAt(), "Registration cancelled", "CANCELLED"));
        }
        if (feedback != null) {
            entries.add(new TimelineEntry(feedback.getSubmittedAt(), "Feedback submitted (" + feedback.getRating() + "/5)", "FEEDBACK"));
        }
        entries.sort(Comparator.comparing(TimelineEntry::at));
        return entries;
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String v = value.replace("\"", "\"\"");
        // Prevent CSV/Excel formula injection
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v + "\"";
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
