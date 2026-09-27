package com.eventsphere.service;

import com.eventsphere.dto.EventDtos.EventDetailDto;
import com.eventsphere.dto.EventDtos.EventDto;
import com.eventsphere.dto.EventDtos.EventRequest;
import com.eventsphere.dto.EventDtos.MyRegistrationDto;
import com.eventsphere.entity.*;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.NotFoundException;
import com.eventsphere.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Event lifecycle: DRAFT -> PUBLISHED -> COMPLETED, DRAFT/PUBLISHED -> CANCELLED (rules R1-R7).
 */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventSessionRepository sessionRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final WaitlistService waitlistService;
    private final EventMapper mapper;
    private final AccessPolicy access;
    private final Clock clock;

    public EventService(EventRepository eventRepository, EventSessionRepository sessionRepository,
                        RegistrationRepository registrationRepository, AttendanceRepository attendanceRepository,
                        FeedbackRepository feedbackRepository, WaitlistService waitlistService,
                        EventMapper mapper, AccessPolicy access, Clock clock) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.feedbackRepository = feedbackRepository;
        this.waitlistService = waitlistService;
        this.mapper = mapper;
        this.access = access;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ queries

    /**
     * Participants see published and completed events; organizers see their own events; admins see all.
     */
    @Transactional(readOnly = true)
    public List<EventDto> list(User user, EventStatus status, EventCategory category, String query) {
        LocalDateTime now = now();
        List<Event> events = switch (user.getRole()) {
            case ADMIN -> eventRepository.findAllByOrderByStartDateTimeDesc();
            case ORGANIZER -> eventRepository.findByOrganizerIdOrderByStartDateTimeDesc(user.getId());
            case PARTICIPANT -> eventRepository.findByStatusInOrderByStartDateTimeAsc(
                    List.of(EventStatus.PUBLISHED, EventStatus.COMPLETED));
        };
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Event> filtered = events.stream()
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> category == null || e.getCategory() == category)
                .filter(e -> q.isEmpty() || e.getTitle().toLowerCase(Locale.ROOT).contains(q)
                        || e.getVenue().toLowerCase(Locale.ROOT).contains(q))
                .toList();
        if (user.getRole() == Role.PARTICIPANT) {
            // Upcoming/live first (soonest on top), then past events (most recent on top)
            Stream<Event> upcoming = filtered.stream()
                    .filter(e -> e.getStatus() == EventStatus.PUBLISHED && e.getEndDateTime().isAfter(now))
                    .sorted(Comparator.comparing(Event::getStartDateTime));
            Stream<Event> past = filtered.stream()
                    .filter(e -> e.getStatus() == EventStatus.COMPLETED || !e.getEndDateTime().isAfter(now))
                    .sorted(Comparator.comparing(Event::getStartDateTime).reversed());
            filtered = Stream.concat(upcoming, past).toList();
        }
        return mapper.toDtos(filtered, now);
    }

    /** Public landing page: next published events that still accept registrations. */
    @Transactional(readOnly = true)
    public List<EventDto> featured(int limit) {
        LocalDateTime now = now();
        List<Event> events = eventRepository.findByStatusInOrderByStartDateTimeAsc(List.of(EventStatus.PUBLISHED)).stream()
                .filter(e -> e.getEndDateTime().isAfter(now))
                .limit(limit)
                .toList();
        return mapper.toDtos(events, now);
    }

    @Transactional(readOnly = true)
    public EventDetailDto detail(Long id, User user) {
        LocalDateTime now = now();
        Event event = find(id);
        boolean canManage = access.canManage(event, user);
        if (!canManage && event.getStatus() == EventStatus.DRAFT) {
            throw new NotFoundException("Event", id); // drafts are invisible to everyone but their owner
        }

        MyRegistrationDto myRegistration = null;
        boolean canRegister = false;
        boolean canGiveFeedback = false;
        boolean feedbackGiven = false;

        if (access.isParticipant(user)) {
            Registration reg = registrationRepository.findByEventIdAndParticipantId(id, user.getId()).orElse(null);
            boolean active = reg != null && reg.getStatus() != RegistrationStatus.CANCELLED;
            canRegister = event.isRegistrationOpen(now) && !active;
            feedbackGiven = feedbackRepository.existsByEventIdAndParticipantId(id, user.getId());
            if (reg != null) {
                boolean attended = attendanceRepository.existsByRegistrationId(reg.getId());
                canGiveFeedback = event.getStatus() == EventStatus.COMPLETED
                        && reg.getStatus() == RegistrationStatus.CONFIRMED && attended && !feedbackGiven;
                myRegistration = new MyRegistrationDto(reg.getId(), reg.getStatus(),
                        waitlistService.waitlistPosition(reg),
                        reg.getStatus() == RegistrationStatus.CONFIRMED ? reg.getTicketCode() : null,
                        active && event.getStatus() == EventStatus.PUBLISHED && !event.hasStarted(now));
            }
        }
        return new EventDetailDto(mapper.toDto(event, now), mapper.sessions(event), myRegistration,
                canManage, canRegister, canGiveFeedback, feedbackGiven);
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public EventDto create(EventRequest req, User organizer) {
        LocalDateTime now = now();
        validateDates(req);
        if (!req.startDateTime().isAfter(now)) {
            throw new BusinessRuleException("An event must start in the future");
        }
        Event event = new Event(req.title().trim(), req.description(), req.category(), req.venue().trim(),
                req.startDateTime(), req.endDateTime(), req.registrationDeadline(), req.capacity(), organizer);
        return mapper.toDto(eventRepository.save(event), now);
    }

    @Transactional
    public EventDto update(Long id, EventRequest req, User user) {
        LocalDateTime now = now();
        Event event = eventRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Event", id));
        access.requireManage(event, user);
        requireOpenForChanges(event);
        validateDates(req);

        // R5: existing sessions must still fit inside the new event window
        for (EventSession s : sessionRepository.findByEventIdOrderByStartTimeAsc(id)) {
            if (s.getStartTime().isBefore(req.startDateTime()) || s.getEndTime().isAfter(req.endDateTime())) {
                throw new BusinessRuleException("Session '" + s.getTitle() + "' (" + Fmt.dateTime(s.getStartTime())
                        + ") would fall outside the new event dates. Move the session first.");
            }
        }
        // R3: capacity cannot drop below seats already given away
        long confirmed = registrationRepository.countByEventIdAndStatus(id, RegistrationStatus.CONFIRMED);
        if (req.capacity() < confirmed) {
            throw new BusinessRuleException("Capacity cannot be reduced to " + req.capacity() + ": "
                    + confirmed + " participants are already confirmed");
        }
        boolean deadlineChanged = !req.registrationDeadline().equals(event.getRegistrationDeadline());
        if (event.getStatus() == EventStatus.PUBLISHED && deadlineChanged && !req.registrationDeadline().isAfter(now)) {
            throw new BusinessRuleException("The new registration deadline is already in the past");
        }

        event.setTitle(req.title().trim());
        event.setDescription(req.description());
        event.setCategory(req.category());
        event.setVenue(req.venue().trim());
        event.setStartDateTime(req.startDateTime());
        event.setEndDateTime(req.endDateTime());
        event.setRegistrationDeadline(req.registrationDeadline());
        event.setCapacity(req.capacity());

        // R4: extra capacity goes to the waitlist, first come first served
        if (event.getStatus() == EventStatus.PUBLISHED) {
            waitlistService.fillOpenSeats(event, now);
        }
        return mapper.toDto(event, now);
    }

    /** R6: only drafts can be deleted - anything published has history that must be kept. */
    @Transactional
    public void delete(Long id, User user) {
        Event event = find(id);
        access.requireManage(event, user);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only draft events can be deleted. Cancel the event instead.");
        }
        eventRepository.delete(event);
    }

    /** R2: publish only a complete event (has sessions, deadline in the future). */
    @Transactional
    public EventDto publish(Long id, User user) {
        LocalDateTime now = now();
        Event event = find(id);
        access.requireManage(event, user);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only draft events can be published (current status: " + event.getStatus() + ")");
        }
        if (sessionRepository.countByEventId(id) == 0) {
            throw new BusinessRuleException("Cannot publish: add at least one session to the agenda first");
        }
        if (!event.getRegistrationDeadline().isAfter(now)) {
            throw new BusinessRuleException("Cannot publish: the registration deadline ("
                    + Fmt.dateTime(event.getRegistrationDeadline()) + ") has already passed");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return mapper.toDto(event, now);
    }

    @Transactional
    public EventDto cancel(Long id, User user) {
        Event event = find(id);
        access.requireManage(event, user);
        if (event.isClosed()) {
            throw new BusinessRuleException("This event is already " + event.getStatus().name().toLowerCase());
        }
        event.setStatus(EventStatus.CANCELLED);
        return mapper.toDto(event, now());
    }

    /** R7: an event can be closed only once it has started; this opens feedback (R19). */
    @Transactional
    public EventDto complete(Long id, User user) {
        LocalDateTime now = now();
        Event event = find(id);
        access.requireManage(event, user);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Only published events can be completed");
        }
        if (!event.hasStarted(now)) {
            throw new BusinessRuleException("The event has not started yet (starts " + Fmt.dateTime(event.getStartDateTime()) + ")");
        }
        event.setStatus(EventStatus.COMPLETED);
        return mapper.toDto(event, now);
    }

    // ------------------------------------------------------------------ helpers

    public Event find(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event", id));
    }

    static void requireOpenForChanges(Event event) {
        if (event.isClosed()) {
            throw new BusinessRuleException("This event is " + event.getStatus().name().toLowerCase()
                    + " and can no longer be changed");
        }
    }

    /** R1 */
    private static void validateDates(EventRequest req) {
        if (!req.endDateTime().isAfter(req.startDateTime())) {
            throw new BusinessRuleException("The event must end after it starts");
        }
        if (req.registrationDeadline().isAfter(req.startDateTime())) {
            throw new BusinessRuleException("The registration deadline must be on or before the event start");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
