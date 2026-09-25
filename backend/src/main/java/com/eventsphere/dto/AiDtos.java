package com.eventsphere.dto;

import com.eventsphere.entity.EventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record DescriptionRequest(
            @NotBlank(message = "Give the event a title first") @Size(max = 200) String title,
            EventCategory category,
            @Size(max = 200) String venue,
            @Size(max = 200) String audience,
            @Size(max = 1000) String highlights,
            @Size(max = 40) String tone) {
    }

    /** source = GEMINI when the model answered, FALLBACK when the built-in generator was used. */
    public record AiText(String text, String source) {
    }

    public record FeedbackSummary(
            String sentiment,
            String headline,
            List<String> strengths,
            List<String> improvements,
            List<String> actionItems,
            String source,
            LocalDateTime generatedAt) {
    }

    public record AiStatus(boolean geminiConfigured, String model) {
    }
}
