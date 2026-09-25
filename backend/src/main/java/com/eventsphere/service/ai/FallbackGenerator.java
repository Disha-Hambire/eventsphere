package com.eventsphere.service.ai;

import com.eventsphere.dto.AiDtos.DescriptionRequest;
import com.eventsphere.dto.AiDtos.FeedbackSummary;
import com.eventsphere.entity.EventCategory;
import com.eventsphere.entity.Feedback;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Rule-based stand-ins for the AI features, used when Gemini is not configured or unavailable.
 * The feedback summary does simple theme detection over comments plus rating statistics.
 */
@Component
public class FallbackGenerator {

    /** A feedback theme, the words that signal it, and what to do when people complain about it. */
    private record Theme(String name, List<String> keywords, String action) {
    }

    private static final List<Theme> THEMES = List.of(
            new Theme("Content & speakers", List.of("speaker", "talk", "content", "keynote", "session", "insight", "topic", "knowledge"),
                    "Brief speakers on the audience level and ask for more real-world examples"),
            new Theme("Hands-on learning", List.of("hands-on", "hands on", "lab", "practical", "demo", "exercise", "workshop"),
                    "Allocate more time to hands-on labs and share setup instructions in advance"),
            new Theme("Venue & logistics", List.of("venue", "seat", "wifi", "wi-fi", "audio", "sound", "mic", "hall", "parking", "crowd", "ac ", "projector"),
                    "Run a venue check (seating, audio, Wi-Fi, projector) the day before the event"),
            new Theme("Time management", List.of("time", "late", "delay", "schedule", "overran", "rushed", "long", "short"),
                    "Add buffer time between sessions and have a moderator enforce timings"),
            new Theme("Networking", List.of("network", "connect", "people", "peers", "community"),
                    "Add a dedicated networking slot with icebreakers"),
            new Theme("Food & breaks", List.of("food", "lunch", "coffee", "snack", "tea", "break", "catering"),
                    "Review catering quantity and break timings"));

    private static final List<String> NEGATIVE_CUES = List.of(
            "not ", "no ", "poor", "bad", "late", "slow", "crowded", "issue", "problem", "too ", "could be better",
            "boring", "lacking", "missing", "difficult", "confusing", "delay", "overran", "rushed", "weak", "noisy", "cold", "hot",
            "would like", "wish", "next time", "should", "needs ");

    public String eventDescription(DescriptionRequest req) {
        EventCategory category = req.category() == null ? EventCategory.CONFERENCE : req.category();
        String hook = switch (category) {
            case CONFERENCE -> "Join industry leaders and practitioners for a day of ideas that move the needle.";
            case CORPORATE -> "An exclusive gathering designed to align, inspire and energise our teams.";
            case COLLEGE -> "Calling all students: this is your chance to learn, build and stand out.";
            case WORKSHOP -> "Roll up your sleeves for an immersive, hands-on learning experience.";
            case MEETUP -> "Meet the community, swap stories and learn from people who have been there.";
            case WEBINAR -> "Tune in from anywhere for a focused session packed with practical insight.";
            case CULTURAL -> "Celebrate creativity, culture and community in an unforgettable experience.";
            case SPORTS -> "Bring your energy and team spirit to a day of friendly competition.";
        };
        String audience = isBlank(req.audience()) ? "curious minds and professionals" : req.audience().trim();
        String venue = isBlank(req.venue()) ? "" : " at " + req.venue().trim();
        List<String> gains = new ArrayList<>();
        if (!isBlank(req.highlights())) {
            for (String h : req.highlights().split("[,;\\n]")) {
                if (!h.isBlank() && gains.size() < 3) {
                    gains.add(capitalize(h.trim()));
                }
            }
        }
        List<String> defaults = List.of("Practical takeaways you can apply immediately",
                "Conversations with experts and peers", "New connections and fresh perspectives");
        for (String d : defaults) {
            if (gains.size() < 3) {
                gains.add(d);
            }
        }
        return hook + "\n\n"
                + req.title().trim() + " brings together " + audience + venue
                + " for an experience built around learning, collaboration and real outcomes. "
                + "Every session is curated to be engaging, relevant and worth your time.\n\n"
                + "What you'll gain:\n- " + String.join("\n- ", gains) + "\n\n"
                + "Seats are limited - register now to secure your spot.";
    }

    public FeedbackSummary feedbackSummary(List<Feedback> feedback, LocalDateTime now) {
        int n = feedback.size();
        double avg = feedback.stream().mapToInt(Feedback::getRating).average().orElse(0);
        double content = feedback.stream().mapToInt(Feedback::getContentRating).average().orElse(0);
        double organisation = feedback.stream().mapToInt(Feedback::getOrganizationRating).average().orElse(0);
        double recommend = n == 0 ? 0 : feedback.stream().filter(Feedback::isWouldRecommend).count() * 100.0 / n;

        String sentiment = avg >= 4 && recommend >= 70 ? "POSITIVE" : avg < 3 ? "NEGATIVE" : "MIXED";
        String headline = String.format(Locale.ENGLISH,
                "%d attendees rated the event %.1f/5 on average and %.0f%% would recommend it.", n, avg, recommend);

        Map<Theme, int[]> counts = new LinkedHashMap<>(); // [positive, negative]
        for (Feedback f : feedback) {
            if (f.getComments() == null) {
                continue;
            }
            for (String clause : f.getComments().toLowerCase(Locale.ROOT).split("[.;!?]|\\bbut\\b|\\bhowever\\b|\\bthough\\b")) {
                String c = " " + clause.trim() + " ";
                for (Theme t : THEMES) {
                    if (t.keywords().stream().anyMatch(c::contains)) {
                        boolean negative = NEGATIVE_CUES.stream().anyMatch(c::contains) || f.getRating() <= 2;
                        counts.computeIfAbsent(t, k -> new int[2])[negative ? 1 : 0]++;
                    }
                }
            }
        }

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        counts.entrySet().stream()
                .filter(e -> e.getValue()[0] > 0)
                .sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
                .limit(3)
                .forEach(e -> strengths.add(e.getKey().name() + " was appreciated in " + e.getValue()[0] + " comment(s)"));
        counts.entrySet().stream()
                .filter(e -> e.getValue()[1] > 0)
                .sorted((a, b) -> b.getValue()[1] - a.getValue()[1])
                .limit(3)
                .forEach(e -> {
                    improvements.add(e.getKey().name() + " was raised as a concern in " + e.getValue()[1] + " comment(s)");
                    actions.add(e.getKey().action());
                });

        if (content >= 4) {
            strengths.add(String.format(Locale.ENGLISH, "Strong content quality (%.1f/5)", content));
        } else if (content > 0 && content < 3.5) {
            improvements.add(String.format(Locale.ENGLISH, "Content quality scored only %.1f/5", content));
            actions.add("Review the agenda with speakers to make sessions more relevant");
        }
        if (organisation >= 4) {
            strengths.add(String.format(Locale.ENGLISH, "Well organised event (%.1f/5)", organisation));
        } else if (organisation > 0 && organisation < 3.5) {
            improvements.add(String.format(Locale.ENGLISH, "Organisation scored only %.1f/5", organisation));
            actions.add("Assign a volunteer lead for registration desk, signage and timekeeping");
        }
        if (improvements.isEmpty()) {
            improvements.add("No significant concerns were raised");
        }
        if (actions.isEmpty()) {
            actions.add("Keep the current format and survey attendees for topics for the next edition");
        }
        return new FeedbackSummary(sentiment, headline, trim(strengths, 4), trim(improvements, 4),
                trim(new ArrayList<>(new LinkedHashSet<>(actions)), 3), "FALLBACK", now);
    }

    private static List<String> trim(List<String> list, int max) {
        return list.size() <= max ? list : new ArrayList<>(list.subList(0, max));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
