package com.eventsphere.dto;

import com.eventsphere.entity.Feedback;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    public record FeedbackRequest(
            @NotNull(message = "Overall rating is required") @Min(1) @Max(5) Integer rating,
            @NotNull(message = "Content rating is required") @Min(1) @Max(5) Integer contentRating,
            @NotNull(message = "Organisation rating is required") @Min(1) @Max(5) Integer organizationRating,
            @NotNull(message = "Please tell us if you would recommend this event") Boolean wouldRecommend,
            @Size(max = 2000, message = "Comments can be at most 2000 characters") String comments) {
    }

    public record FeedbackDto(Long id, Long eventId, String participantName, int rating, int contentRating,
                              int organizationRating, boolean wouldRecommend, String comments,
                              LocalDateTime submittedAt) {

        public static FeedbackDto from(Feedback f) {
            return new FeedbackDto(f.getId(), f.getEvent().getId(), f.getParticipant().getFullName(),
                    f.getRating(), f.getContentRating(), f.getOrganizationRating(), f.isWouldRecommend(),
                    f.getComments(), f.getSubmittedAt());
        }
    }
}
