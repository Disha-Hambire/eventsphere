package com.eventsphere.service;

import com.eventsphere.dto.AttendanceDtos.AttendanceRow;
import com.eventsphere.dto.AttendanceDtos.CheckInOutcome;
import com.eventsphere.dto.AttendanceDtos.CheckInRequest;
import com.eventsphere.dto.AttendanceDtos.CheckInResult;
import com.eventsphere.entity.*;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.NotFoundException;
import com.eventsphere.repository.AttendanceRepository;
import com.eventsphere.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Session-wise attendance (R17, R18): QR ticket scanning at the door plus a manual attendance sheet.
 */
@Service
public class AttendanceService {

    /** Check-in opens this many minutes before a session starts. */
    static final int CHECK_IN_OPENS_MINUTES = 30;

    private final AttendanceRepository attendanceRepository;
    private final RegistrationRepository registrationRepository;
    private final SessionService sessionService;
    private final AccessPolicy access;
    private final Clock clock;

    public AttendanceService(AttendanceRepository attendanceRepository, RegistrationRepository registrationRepository,
                             SessionService sessionService, AccessPolicy access, Clock clock) {
        this.attendanceRepository = attendanceRepository;
        this.registrationRepository = registrationRepository;
        this.sessionService = sessionService;
        this.access = access;
        this.clock = clock;
    }

    /** Called by the QR scanner. Scanning the same ticket twice is not an error - it tells the staff who it is. */
    @Transactional
    public CheckInResult checkInByTicket(CheckInRequest req, User staff) {
        EventSession session = sessionService.find(req.sessionId());
        String code = TicketCodeGenerator.normalize(req.ticketCode());
        Registration registration = registrationRepository.findByTicketCode(code)
                .orElseThrow(() -> new NotFoundException("Invalid ticket: no registration found for code " + code));
        return checkIn(session, registration, staff, Attendance.Method.QR);
    }

    @Transactional
    public CheckInResult markPresent(Long sessionId, Long registrationId, User staff) {
        EventSession session = sessionService.find(sessionId);
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration", registrationId));
        return checkIn(session, registration, staff, Attendance.Method.MANUAL);
    }

    @Transactional
    public void markAbsent(Long sessionId, Long registrationId, User staff) {
        EventSession session = sessionService.find(sessionId);
        Event event = session.getEvent();
        access.requireManage(event, staff);
        requireCheckInOpen(event);
        attendanceRepository.findByRegistrationIdAndSessionId(registrationId, sessionId)
                .ifPresent(attendanceRepository::delete);
    }

    /** The attendance sheet: every confirmed participant with present/absent for this session. */
    @Transactional(readOnly = true)
    public List<AttendanceRow> sheet(Long sessionId, User staff) {
        EventSession session = sessionService.find(sessionId);
        access.requireManage(session.getEvent(), staff);
        Map<Long, Attendance> bySession = attendanceRepository.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(a -> a.getRegistration().getId(), Function.identity()));
        return registrationRepository
                .findByEventIdAndStatusOrderByRegisteredAtAsc(session.getEvent().getId(), RegistrationStatus.CONFIRMED)
                .stream()
                .map(r -> {
                    Attendance a = bySession.get(r.getId());
                    User p = r.getParticipant();
                    return new AttendanceRow(r.getId(), p.getId(), p.getFullName(), p.getEmail(), r.getTicketCode(),
                            a != null, a == null ? null : a.getCheckedInAt(), a == null ? null : a.getMethod());
                })
                .toList();
    }

    private CheckInResult checkIn(EventSession session, Registration registration, User staff, Attendance.Method method) {
        LocalDateTime now = LocalDateTime.now(clock);
        Event event = session.getEvent();
        access.requireManage(event, staff);
        requireCheckInOpen(event);

        if (!registration.getEvent().getId().equals(event.getId())) {
            throw new BusinessRuleException("This ticket is for '" + registration.getEvent().getTitle()
                    + "', not '" + event.getTitle() + "'");
        }
        String name = registration.getParticipant().getFullName();
        // R17
        switch (registration.getStatus()) {
            case WAITLISTED -> throw new BusinessRuleException(name + " is on the waitlist and has no confirmed seat");
            case CANCELLED -> throw new BusinessRuleException(name + "'s registration was cancelled");
            case CONFIRMED -> { /* ok */ }
        }
        // R18: check-in window = 30 min before the session until the event ends
        LocalDateTime opensAt = session.getStartTime().minusMinutes(CHECK_IN_OPENS_MINUTES);
        if (now.isBefore(opensAt)) {
            throw new BusinessRuleException("Check-in for '" + session.getTitle() + "' opens at "
                    + Fmt.dateTime(opensAt));
        }
        if (now.isAfter(event.getEndDateTime())) {
            throw new BusinessRuleException("The event has ended; attendance can no longer be recorded");
        }

        var existing = attendanceRepository.findByRegistrationIdAndSessionId(registration.getId(), session.getId());
        if (existing.isPresent()) {
            return result(CheckInOutcome.ALREADY_CHECKED_IN, name + " was already checked in at "
                    + Fmt.time(existing.get().getCheckedInAt()), registration, session, existing.get().getCheckedInAt());
        }
        Attendance saved = attendanceRepository.save(new Attendance(registration, session, now, method));
        return result(CheckInOutcome.CHECKED_IN, "Welcome, " + name + "! Checked in to " + session.getTitle(),
                registration, session, saved.getCheckedInAt());
    }

    private static void requireCheckInOpen(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Attendance is locked: the event is " + event.getStatus().name().toLowerCase());
        }
    }

    private static CheckInResult result(CheckInOutcome outcome, String message, Registration r,
                                        EventSession s, LocalDateTime at) {
        return new CheckInResult(outcome, message, r.getId(), r.getParticipant().getFullName(),
                r.getParticipant().getEmail(), r.getTicketCode(), s.getTitle(), at);
    }
}
