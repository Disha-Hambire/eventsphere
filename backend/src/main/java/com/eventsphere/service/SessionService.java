package com.eventsphere.service;

import com.eventsphere.dto.SessionDtos.SessionDto;
import com.eventsphere.dto.SessionDtos.SessionRequest;
import com.eventsphere.entity.Event;
import com.eventsphere.entity.EventSession;
import com.eventsphere.entity.Speaker;
import com.eventsphere.entity.User;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.NotFoundException;
import com.eventsphere.repository.AttendanceRepository;
import com.eventsphere.repository.EventSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agenda management with scheduling rules R8 (inside event window), R9 (speaker double-booking)
 * and R10 (room double-booking).
 */
@Service
public class SessionService {

    private final EventSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventService eventService;
    private final SpeakerService speakerService;
    private final AccessPolicy access;

    public SessionService(EventSessionRepository sessionRepository, AttendanceRepository attendanceRepository,
                          EventService eventService, SpeakerService speakerService, AccessPolicy access) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.eventService = eventService;
        this.speakerService = speakerService;
        this.access = access;
    }

    @Transactional
    public SessionDto create(Long eventId, SessionRequest req, User user) {
        Event event = eventService.find(eventId);
        access.requireManage(event, user);
        EventService.requireOpenForChanges(event);
        Speaker speaker = req.speakerId() == null ? null : speakerService.find(req.speakerId());
        validate(event, req, speaker, null);

        EventSession session = new EventSession(event, req.title().trim(), req.description(), speaker,
                blankToNull(req.room()), req.startTime(), req.endTime());
        event.getSessions().add(session);
        return SessionDto.from(sessionRepository.save(session), 0);
    }

    @Transactional
    public SessionDto update(Long sessionId, SessionRequest req, User user) {
        EventSession session = find(sessionId);
        Event event = session.getEvent();
        access.requireManage(event, user);
        EventService.requireOpenForChanges(event);
        Speaker speaker = req.speakerId() == null ? null : speakerService.find(req.speakerId());
        validate(event, req, speaker, sessionId);

        session.setTitle(req.title().trim());
        session.setDescription(req.description());
        session.setSpeaker(speaker);
        session.setRoom(blankToNull(req.room()));
        session.setStartTime(req.startTime());
        session.setEndTime(req.endTime());
        return SessionDto.from(session, attendanceRepository.countBySessionId(sessionId));
    }

    @Transactional
    public void delete(Long sessionId, User user) {
        EventSession session = find(sessionId);
        Event event = session.getEvent();
        access.requireManage(event, user);
        EventService.requireOpenForChanges(event);
        if (attendanceRepository.countBySessionId(sessionId) > 0) {
            throw new BusinessRuleException("Attendance has already been recorded for '" + session.getTitle()
                    + "', so it cannot be deleted");
        }
        event.getSessions().remove(session);
        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> listForEvent(Long eventId) {
        return sessionRepository.findByEventIdOrderByStartTimeAsc(eventId).stream()
                .map(s -> SessionDto.from(s, attendanceRepository.countBySessionId(s.getId())))
                .toList();
    }

    EventSession find(Long id) {
        return sessionRepository.findById(id).orElseThrow(() -> new NotFoundException("Session", id));
    }

    private void validate(Event event, SessionRequest req, Speaker speaker, Long excludeId) {
        // R8
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BusinessRuleException("A session must end after it starts");
        }
        if (req.startTime().isBefore(event.getStartDateTime()) || req.endTime().isAfter(event.getEndDateTime())) {
            throw new BusinessRuleException("The session must be within the event time ("
                    + Fmt.dateTime(event.getStartDateTime()) + " to " + Fmt.dateTime(event.getEndDateTime()) + ")");
        }
        // R9
        if (speaker != null) {
            List<EventSession> clashes = sessionRepository.findSpeakerClashes(
                    speaker.getId(), req.startTime(), req.endTime(), excludeId);
            if (!clashes.isEmpty()) {
                EventSession c = clashes.get(0);
                throw new BusinessRuleException(speaker.getFullName() + " is already speaking at '" + c.getTitle()
                        + "' (" + c.getEvent().getTitle() + ", " + Fmt.time(c.getStartTime()) + " - "
                        + Fmt.time(c.getEndTime()) + ")");
            }
        }
        // R10
        String room = blankToNull(req.room());
        if (room != null) {
            List<EventSession> clashes = sessionRepository.findRoomClashes(
                    event.getId(), room, req.startTime(), req.endTime(), excludeId);
            if (!clashes.isEmpty()) {
                EventSession c = clashes.get(0);
                throw new BusinessRuleException("Room '" + room + "' is already booked for '" + c.getTitle() + "' ("
                        + Fmt.time(c.getStartTime()) + " - " + Fmt.time(c.getEndTime()) + ")");
            }
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
