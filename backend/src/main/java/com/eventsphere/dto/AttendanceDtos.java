package com.eventsphere.dto;

import com.eventsphere.entity.Attendance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    public record CheckInRequest(
            @NotBlank(message = "Ticket code is required") String ticketCode,
            @NotNull(message = "Session is required") Long sessionId) {
    }

    public enum CheckInOutcome { CHECKED_IN, ALREADY_CHECKED_IN }

    public record CheckInResult(CheckInOutcome outcome, String message, Long registrationId,
                                String participantName, String participantEmail, String ticketCode,
                                String sessionTitle, LocalDateTime checkedInAt) {
    }

    /** A row of the per-session attendance sheet (all confirmed participants). */
    public record AttendanceRow(Long registrationId, Long participantId, String participantName,
                                String participantEmail, String ticketCode, boolean present,
                                LocalDateTime checkedInAt, Attendance.Method method) {
    }
}
