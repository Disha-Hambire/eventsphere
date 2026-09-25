package com.eventsphere.service.ai;

import com.eventsphere.dto.AiDtos.DescriptionRequest;
import com.eventsphere.dto.AiDtos.FeedbackSummary;
import com.eventsphere.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit tests - no Spring context needed. */
class FallbackGeneratorTest {

    private final FallbackGenerator generator = new FallbackGenerator();
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 0);

    private Feedback feedback(int rating, int content, int org, boolean recommend, String comment) {
        User u = new User("u", "u@test.com", "x", Role.PARTICIPANT);
        Event e = new Event("E", null, EventCategory.CONFERENCE, "Hall", now, now.plusHours(8), now.minusDays(1), 10, u);
        return new Feedback(e, u, rating, content, org, recommend, comment, now);
    }

    @Test
    void summaryDetectsPraiseAndComplaints() {
        FeedbackSummary s = generator.feedbackSummary(List.of(
                feedback(5, 5, 4, true, "Amazing keynote speaker and great content."),
                feedback(4, 5, 3, true, "Loved the talks, but the Wi-Fi was poor and the hall was crowded."),
                feedback(3, 4, 2, false, "The schedule overran and lunch was late.")), now);

        assertThat(s.source()).isEqualTo("FALLBACK");
        assertThat(s.headline()).contains("3 attendees");
        assertThat(s.strengths()).anyMatch(x -> x.contains("Content & speakers"));
        assertThat(s.improvements()).anyMatch(x -> x.contains("Venue & logistics"));
        assertThat(s.improvements()).anyMatch(x -> x.contains("Time management"));
        assertThat(s.actionItems()).isNotEmpty();
    }

    @Test
    void descriptionUsesHighlights() {
        String text = generator.eventDescription(new DescriptionRequest("AI Bootcamp", EventCategory.WORKSHOP,
                "Lab 1", "final-year students", "build a chatbot, deploy to cloud", null));
        assertThat(text).contains("AI Bootcamp").contains("Build a chatbot").contains("What you'll gain:");
    }
}
