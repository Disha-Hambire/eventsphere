package com.eventsphere.dto;

import com.eventsphere.entity.EventSession;
import com.eventsphere.entity.Speaker;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class SessionDtos {

    private SessionDtos() {
    }

    public record SessionRequest(
            @NotBlank(message = "Session title is required") @Size(max = 200) String title,
            @Size(max = 2000) String description,
            Long speakerId,
            @Size(max = 100) String room,
            @NotNull(message = "Session start time is required") LocalDateTime startTime,
            @NotNull(message = "Session end time is required") LocalDateTime endTime) {
    }

    public record SessionDto(Long id, Long eventId, String title, String description,
                             Long speakerId, String speakerName, String speakerDesignation, String speakerOrganization,
                             String room, LocalDateTime startTime, LocalDateTime endTime, long attendanceCount) {

        public static SessionDto from(EventSession s, long attendanceCount) {
            Speaker sp = s.getSpeaker();
            return new SessionDto(s.getId(), s.getEvent().getId(), s.getTitle(), s.getDescription(),
                    sp == null ? null : sp.getId(),
                    sp == null ? null : sp.getFullName(),
                    sp == null ? null : sp.getDesignation(),
                    sp == null ? null : sp.getOrganization(),
                    s.getRoom(), s.getStartTime(), s.getEndTime(), attendanceCount);
        }
    }
}
