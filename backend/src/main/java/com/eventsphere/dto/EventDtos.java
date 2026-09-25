package com.eventsphere.dto;

import com.eventsphere.entity.EventCategory;
import com.eventsphere.entity.EventStatus;
import com.eventsphere.entity.RegistrationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventRequest(
            @NotBlank(message = "Title is required") @Size(max = 200) String title,
            @Size(max = 5000, message = "Description can be at most 5000 characters") String description,
            @NotNull(message = "Category is required") EventCategory category,
            @NotBlank(message = "Venue is required") @Size(max = 200) String venue,
            @NotNull(message = "Start date/time is required") LocalDateTime startDateTime,
            @NotNull(message = "End date/time is required") LocalDateTime endDateTime,
            @NotNull(message = "Registration deadline is required") LocalDateTime registrationDeadline,
            @NotNull(message = "Capacity is required") @Min(value = 1, message = "Capacity must be at least 1")
            @Max(value = 100000, message = "Capacity is too large") Integer capacity) {
    }

    /** Card / list view of an event, including live seat numbers. */
    public record EventDto(
            Long id,
            String title,
            String description,
            EventCategory category,
            String venue,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            LocalDateTime registrationDeadline,
            int capacity,
            EventStatus status,
            String phase,
            long confirmedCount,
            long waitlistCount,
            long seatsLeft,
            boolean registrationOpen,
            long sessionCount,
            Long organizerId,
            String organizerName) {
    }

    /** The current participant's relationship with an event. */
    public record MyRegistrationDto(Long id, RegistrationStatus status, Integer waitlistPosition,
                                    String ticketCode, boolean canCancel) {
    }

    public record EventDetailDto(
            EventDto event,
            List<SessionDtos.SessionDto> sessions,
            MyRegistrationDto myRegistration,
            boolean canManage,
            boolean canRegister,
            boolean canGiveFeedback,
            boolean feedbackGiven) {
    }
}
