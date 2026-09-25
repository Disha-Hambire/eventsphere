package com.eventsphere.dto;

import com.eventsphere.entity.EventCategory;
import com.eventsphere.entity.EventStatus;
import com.eventsphere.entity.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.List;

public final class RegistrationDtos {

    private RegistrationDtos() {
    }

    /** One step in a registration's history, e.g. "Joined waitlist" -> "Promoted" -> "Checked in". */
    public record TimelineEntry(LocalDateTime at, String label, String type) {
    }

    public record RegistrationDto(
            Long id,
            Long eventId,
            String eventTitle,
            EventCategory eventCategory,
            String venue,
            LocalDateTime eventStart,
            LocalDateTime eventEnd,
            EventStatus eventStatus,
            Long participantId,
            String participantName,
            String participantEmail,
            RegistrationStatus status,
            String ticketCode,
            LocalDateTime registeredAt,
            LocalDateTime confirmedAt,
            LocalDateTime cancelledAt,
            boolean promotedFromWaitlist,
            Integer waitlistPosition,
            long sessionsAttended,
            boolean feedbackGiven,
            boolean canCancel,
            boolean canGiveFeedback,
            List<TimelineEntry> timeline) {
    }

    /** Returned after register/cancel so the UI can tell the user exactly what happened. */
    public record RegistrationResult(RegistrationDto registration, String message, RegistrationDto promoted) {
    }
}
