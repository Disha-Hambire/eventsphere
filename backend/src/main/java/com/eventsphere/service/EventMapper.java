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
import java.util.List;

/**
 * Converts events to DTOs, enriching them with live seat numbers and a human "phase".
 */
@Component
public class EventMapper {

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
        long confirmed = registrationRepository.countByEventIdAndStatus(e.getId(), RegistrationStatus.CONFIRMED);
        long waitlisted = registrationRepository.countByEventIdAndStatus(e.getId(), RegistrationStatus.WAITLISTED);
        return new EventDto(
                e.getId(), e.getTitle(), e.getDescription(), e.getCategory(), e.getVenue(),
                e.getStartDateTime(), e.getEndDateTime(), e.getRegistrationDeadline(), e.getCapacity(),
                e.getStatus(), phase(e, now), confirmed, waitlisted,
                Math.max(0, e.getCapacity() - confirmed),
                e.isRegistrationOpen(now),
                sessionRepository.countByEventId(e.getId()),
                e.getOrganizer().getId(), e.getOrganizer().getFullName());
    }

    public List<SessionDto> sessions(Event e) {
        return sessionRepository.findByEventIdOrderByStartTimeAsc(e.getId()).stream()
                .map(s -> SessionDto.from(s, attendanceRepository.countBySessionId(s.getId())))
                .toList();
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
