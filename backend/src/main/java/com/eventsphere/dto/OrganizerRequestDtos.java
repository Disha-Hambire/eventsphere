package com.eventsphere.dto;

import com.eventsphere.entity.OrganizerRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class OrganizerRequestDtos {

    private OrganizerRequestDtos() {
    }

    public record SubmitRequest(
            @Size(max = 160) String organization,
            @NotBlank(message = "Tell us why you want to organize events")
            @Size(min = 20, max = 1000, message = "Please write 20-1000 characters") String reason) {
    }

    public record DecisionRequest(@Size(max = 500) String note) {
    }

    public record OrganizerRequestDto(Long id, Long userId, String userName, String userEmail, String organization,
                                      String reason, OrganizerRequest.Status status, String adminNote,
                                      LocalDateTime createdAt, LocalDateTime decidedAt, String decidedByName) {

        public static OrganizerRequestDto from(OrganizerRequest r) {
            return new OrganizerRequestDto(r.getId(), r.getUser().getId(), r.getUser().getFullName(),
                    r.getUser().getEmail(), r.getOrganization(), r.getReason(), r.getStatus(), r.getAdminNote(),
                    r.getCreatedAt(), r.getDecidedAt(), r.getDecidedBy() == null ? null : r.getDecidedBy().getFullName());
        }
    }
}
