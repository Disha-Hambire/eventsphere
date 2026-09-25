package com.eventsphere.service.ai;

import com.eventsphere.dto.AiDtos.AiStatus;
import com.eventsphere.dto.AiDtos.AiText;
import com.eventsphere.dto.AiDtos.DescriptionRequest;
import com.eventsphere.dto.AiDtos.FeedbackSummary;
import com.eventsphere.entity.Event;
import com.eventsphere.entity.Feedback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI features. Each feature has a deterministic fallback so the product works (and demos) without an API key.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final GeminiClient gemini;
    private final FallbackGenerator fallback;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AiService(GeminiClient gemini, FallbackGenerator fallback, ObjectMapper objectMapper, Clock clock) {
        this.gemini = gemini;
        this.fallback = fallback;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public AiStatus status() {
        return new AiStatus(gemini.isConfigured(), gemini.model());
    }

    public AiText generateEventDescription(DescriptionRequest req) {
        String prompt = """
                You are a copywriter for a professional event platform.
                Write an engaging event description for the event below.

                Title: %s
                Category: %s
                Venue: %s
                Target audience: %s
                Key highlights: %s
                Tone: %s

                Rules:
                - 120 to 180 words, plain text only (no markdown symbols like # or *).
                - Start with a one-sentence hook, then a short paragraph about what attendees will experience.
                - Then a line "What you'll gain:" followed by 3 short lines each starting with "- ".
                - End with a one-line call to action to register.
                - Do not invent dates, prices, or speaker names.
                """.formatted(req.title(), orDash(req.category()), orDash(req.venue()), orDash(req.audience()),
                orDash(req.highlights()), req.tone() == null || req.tone().isBlank() ? "professional and inviting" : req.tone());

        return gemini.generate(prompt, false, 0.8)
                .map(text -> new AiText(text, "GEMINI"))
                .orElseGet(() -> new AiText(fallback.eventDescription(req), "FALLBACK"));
    }

    public FeedbackSummary summarizeFeedback(Event event, List<Feedback> feedback) {
        LocalDateTime now = LocalDateTime.now(clock);
        StringBuilder data = new StringBuilder();
        for (Feedback f : feedback) {
            data.append("- overall ").append(f.getRating()).append("/5, content ").append(f.getContentRating())
                    .append("/5, organisation ").append(f.getOrganizationRating()).append("/5, recommend: ")
                    .append(f.isWouldRecommend() ? "yes" : "no");
            if (f.getComments() != null) {
                data.append(", comment: \"").append(f.getComments().replace("\"", "'")).append('"');
            }
            data.append('\n');
        }
        String prompt = """
                You are an event analytics assistant. Analyse the participant feedback for the event "%s" (%s)
                and produce an actionable summary for the organizer.

                Feedback (%d responses):
                %s
                Reply ONLY with JSON of this exact shape:
                {"sentiment": "POSITIVE" | "MIXED" | "NEGATIVE",
                 "headline": "one sentence overall verdict",
                 "strengths": ["up to 4 short points"],
                 "improvements": ["up to 4 short points"],
                 "actionItems": ["up to 3 concrete actions for the next event"]}
                Base every point on the feedback above; do not invent facts.
                """.formatted(event.getTitle(), event.getCategory(), feedback.size(), data);

        return gemini.generate(prompt, true, 0.3)
                .flatMap(json -> parseSummary(json, now))
                .orElseGet(() -> fallback.feedbackSummary(feedback, now));
    }

    public String toJson(FeedbackSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialise AI summary", e);
        }
    }

    public FeedbackSummary fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, FeedbackSummary.class);
        } catch (Exception e) {
            log.warn("Stored AI summary is unreadable, ignoring it: {}", e.getMessage());
            return null;
        }
    }

    private java.util.Optional<FeedbackSummary> parseSummary(String json, LocalDateTime now) {
        try {
            String clean = json.strip();
            if (clean.startsWith("```")) { // tolerate fenced output
                clean = clean.replaceAll("^```(json)?", "").replaceAll("```$", "").strip();
            }
            JsonNode n = objectMapper.readTree(clean);
            String sentiment = n.path("sentiment").asText("MIXED").toUpperCase(Locale.ROOT);
            if (!List.of("POSITIVE", "MIXED", "NEGATIVE").contains(sentiment)) {
                sentiment = "MIXED";
            }
            return java.util.Optional.of(new FeedbackSummary(sentiment, n.path("headline").asText(""),
                    list(n.path("strengths")), list(n.path("improvements")), list(n.path("actionItems")),
                    "GEMINI", now));
        } catch (Exception e) {
            log.warn("Could not parse Gemini summary JSON, using fallback: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    private static List<String> list(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> {
                if (!item.asText("").isBlank() && out.size() < 5) {
                    out.add(item.asText().trim());
                }
            });
        }
        return out;
    }

    private static String orDash(Object value) {
        return value == null || value.toString().isBlank() ? "-" : value.toString();
    }
}
