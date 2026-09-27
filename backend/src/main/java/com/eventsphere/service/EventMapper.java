package com.eventsphere.service;

import com.eventsphere.dto.EventDtos.EventDto;
import com.eventsphere.dto.SessionDtos.SessionDto;
import com.eventsphere.entity.Event;
import com.eventsphere.entity.EventStatus;
import com.eventsphere.entity.RegistrationStatus;
import com.eventsphere.repository.AttendanceRepository;
import com.eventsphere.repository.EventSessionRepository;
import com.eventsphere.repository.RegistrationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts events to DTOs, enriching them with live seat numbers and a human "phase".
 * Lists are converted in bulk: two grouped queries for any number of events, instead of three per event
 * (this matters when the database is far away: every query is a network round trip).
 */
@Component
public class EventMapper {

    /** Live numbers for one event. */
    public record Counts(long confirmed, long waitlisted, long sessions) {
        static final Counts NONE = new Counts(0, 0, 0);
    }

    private final RegistrationRepository registrationRepository;
    private final EventSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;

    public EventMapper(RegistrationRepository registrationRepository, EventSessionRepository sessionRepository,
                       AttendanceRepository attendanceRepository) {
        this.registrationRepository = registrationRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public EventDto toDto(Event e, LocalDateTime now) {
        return toDtos(List.of(e), now).get(0);
    }

    public List<EventDto> toDtos(List<Event> events, LocalDateTime now) {
        Map<Long, Counts> counts = counts(events.stream().map(Event::getId).toList());
        return events.stream().map(e -> toDto(e, counts.getOrDefault(e.getId(), Counts.NONE), now)).toList();
    }

    /** Confirmed / waitlisted / session counts for many events in two queries. */
    public Map<Long, Counts> counts(List<Long> eventIds) {
        Map<Long, long[]> acc = new HashMap<>(); // [confirmed, waitlisted, sessions]
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        for (Object[] row : registrationRepository.countByEventAndStatus(eventIds)) {
            long[] c = acc.computeIfAbsent((Long) row[0], k -> new long[3]);
            RegistrationStatus status = (RegistrationStatus) row[1];
            long n = (Long) row[2];
            if (status == RegistrationStatus.CONFIRMED) {
                c[0] = n;
            } else if (status == RegistrationStatus.WAITLISTED) {
                c[1] = n;
            }
        }
        for (Object[] row : sessionRepository.countByEventIds(eventIds)) {
            acc.computeIfAbsent((Long) row[0], k -> new long[3])[2] = (Long) row[1];
        }
        Map<Long, Counts> result = new HashMap<>();
        acc.forEach((id, c) -> result.put(id, new Counts(c[0], c[1], c[2])));
        return result;
    }

    public List<SessionDto> sessions(Event e) {
        return sessionRepository.findByEventIdOrderByStartTimeAsc(e.getId()).stream()
                .map(s -> SessionDto.from(s, attendanceRepository.countBySessionId(s.getId())))
                .toList();
    }

    private static EventDto toDto(Event e, Counts c, LocalDateTime now) {
        return new EventDto(
                e.getId(), e.getTitle(), e.getDescription(), e.getCategory(), e.getVenue(),
                e.getStartDateTime(), e.getEndDateTime(), e.getRegistrationDeadline(), e.getCapacity(),
                e.getStatus(), phase(e, now), c.confirmed(), c.waitlisted(),
                Math.max(0, e.getCapacity() - c.confirmed()),
                e.isRegistrationOpen(now),
                c.sessions(),
                e.getOrganizer().getId(), e.getOrganizer().getFullName());
    }

    /**
     * Status is what the organizer decided; phase is what is happening right now.
     * A PUBLISHED event is UPCOMING, LIVE, or ENDED (awaiting the organizer to complete it).
     */
    public static String phase(Event e, LocalDateTime now) {
        if (e.getStatus() != EventStatus.PUBLISHED) {
            return e.getStatus().name();
        }
        if (now.isBefore(e.getStartDateTime())) {
            return "UPCOMING";
        }
        if (now.isBefore(e.getEndDateTime())) {
            return "LIVE";
        }
        return "ENDED";
    }
}
