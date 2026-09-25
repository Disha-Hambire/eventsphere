package com.eventsphere.service;

import com.eventsphere.dto.AiDtos.FeedbackSummary;
import com.eventsphere.dto.EventDtos;
import com.eventsphere.dto.ReportDtos.*;
import com.eventsphere.entity.*;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.repository.*;
import com.eventsphere.service.ai.AiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Turns operational data into decisions: fill rate, attendance, no-shows, session popularity, ratings.
 */
@Service
public class ReportService {

    private static final int TREND_DAYS = 14;

    private final EventRepository eventRepository;
    private final EventSessionRepository sessionRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final SpeakerRepository speakerRepository;
    private final EventService eventService;
    private final EventMapper mapper;
    private final AccessPolicy access;
    private final AiService aiService;
    private final Clock clock;

    public ReportService(EventRepository eventRepository, EventSessionRepository sessionRepository,
                         RegistrationRepository registrationRepository, AttendanceRepository attendanceRepository,
                         FeedbackRepository feedbackRepository, SpeakerRepository speakerRepository,
                         EventService eventService, EventMapper mapper, AccessPolicy access,
                         AiService aiService, Clock clock) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.feedbackRepository = feedbackRepository;
        this.speakerRepository = speakerRepository;
        this.eventService = eventService;
        this.mapper = mapper;
        this.access = access;
        this.aiService = aiService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EventReport eventReport(Long eventId, User user) {
        Event event = eventService.find(eventId);
        access.requireManage(event, user);
        return buildReport(event);
    }

    /** Generates (or regenerates) the AI summary of feedback and caches it on the event. */
    @Transactional
    public FeedbackSummary generateAiSummary(Long eventId, User user) {
        Event event = eventService.find(eventId);
        access.requireManage(event, user);
        List<Feedback> feedback = feedbackRepository.findByEventIdOrderBySubmittedAtDesc(eventId);
        if (feedback.isEmpty()) {
            throw new BusinessRuleException("There is no feedback to summarise yet");
        }
        FeedbackSummary summary = aiService.summarizeFeedback(event, feedback);
        event.setAiFeedbackSummary(aiService.toJson(summary), summary.generatedAt());
        return summary;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Event> events = user.getRole() == Role.ADMIN
                ? eventRepository.findAllByOrderByStartDateTimeDesc()
                : eventRepository.findByOrganizerIdOrderByStartDateTimeDesc(user.getId());

        long draft = 0, upcoming = 0, live = 0, completed = 0, registrations = 0, waitlisted = 0, checkIns = 0;
        List<Double> attendanceRates = new ArrayList<>();
        List<TopEvent> top = new ArrayList<>();
        List<Integer> ratings = new ArrayList<>();
        Map<String, Long> byCategory = new TreeMap<>();

        for (Event e : events) {
            String phase = EventMapper.phase(e, now);
            switch (phase) {
                case "DRAFT" -> draft++;
                case "UPCOMING" -> upcoming++;
                case "LIVE" -> live++;
                case "COMPLETED" -> completed++;
                default -> { }
            }
            byCategory.merge(e.getCategory().name(), 1L, Long::sum);
            long confirmed = registrationRepository.countByEventIdAndStatus(e.getId(), RegistrationStatus.CONFIRMED);
            long wl = registrationRepository.countByEventIdAndStatus(e.getId(), RegistrationStatus.WAITLISTED);
            registrations += confirmed + wl;
            waitlisted += wl;
            checkIns += attendanceRepository.countByEventId(e.getId());
            if (e.getStatus() == EventStatus.COMPLETED && confirmed > 0) {
                attendanceRates.add(attendanceRepository.countDistinctAttendeesByEventId(e.getId()) * 100.0 / confirmed);
            }
            if (e.getStatus() == EventStatus.PUBLISHED || e.getStatus() == EventStatus.COMPLETED) {
                top.add(new TopEvent(e.getId(), e.getTitle(), confirmed, e.getCapacity(), pct(confirmed, e.getCapacity())));
            }
            feedbackRepository.findByEventIdOrderBySubmittedAtDesc(e.getId()).forEach(f -> ratings.add(f.getRating()));
        }

        LocalDateTime since = now.toLocalDate().minusDays(TREND_DAYS - 1L).atStartOfDay();
        List<LocalDateTime> times = user.getRole() == Role.ADMIN
                ? registrationRepository.findRegistrationTimesSince(since)
                : registrationRepository.findRegistrationTimesSinceForOrganizer(since, user.getId());
        Map<LocalDate, Long> perDay = times.stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));
        List<TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate d = since.toLocalDate().plusDays(i);
            trend.add(new TrendPoint(d, perDay.getOrDefault(d, 0L)));
        }

        List<EventDtos.EventDto> upcomingList = events.stream()
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED && e.getEndDateTime().isAfter(now))
                .sorted(Comparator.comparing(Event::getStartDateTime))
                .limit(5)
                .map(e -> mapper.toDto(e, now))
                .toList();

        return new Dashboard(events.size(), draft, upcoming, live, completed, registrations, waitlisted, checkIns,
                round(attendanceRates.stream().mapToDouble(Double::doubleValue).average().orElse(0)),
                ratings.isEmpty() ? null : round(ratings.stream().mapToInt(Integer::intValue).average().orElse(0)),
                trend,
                byCategory.entrySet().stream().map(en -> new CategoryCount(en.getKey(), en.getValue())).toList(),
                top.stream().sorted(Comparator.comparingDouble(TopEvent::fillRate).reversed()).limit(5).toList(),
                upcomingList);
    }

    @Transactional(readOnly = true)
    public PublicStats publicStats() {
        return new PublicStats(
                eventRepository.count() - eventRepository.countByStatus(EventStatus.DRAFT),
                registrationRepository.count() - registrationRepository.countByStatus(RegistrationStatus.CANCELLED),
                attendanceRepository.count(),
                speakerRepository.count(),
                Optional.ofNullable(feedbackRepository.averageRating()).map(ReportService::round).orElse(null));
    }

    private EventReport buildReport(Event event) {
        Long id = event.getId();
        long confirmed = registrationRepository.countByEventIdAndStatus(id, RegistrationStatus.CONFIRMED);
        long waitlisted = registrationRepository.countByEventIdAndStatus(id, RegistrationStatus.WAITLISTED);
        long cancelled = registrationRepository.countByEventIdAndStatus(id, RegistrationStatus.CANCELLED);
        long attendees = attendanceRepository.countDistinctAttendeesByEventId(id);

        List<SessionStat> sessions = sessionRepository.findByEventIdOrderByStartTimeAsc(id).stream()
                .map(s -> {
                    long attended = attendanceRepository.countBySessionId(s.getId());
                    return new SessionStat(s.getId(), s.getTitle(),
                            s.getSpeaker() == null ? null : s.getSpeaker().getFullName(),
                            s.getStartTime(), attended, pct(attended, confirmed));
                })
                .toList();

        List<Feedback> feedback = feedbackRepository.findByEventIdOrderBySubmittedAtDesc(id);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            int s = star;
            distribution.put(s, feedback.stream().filter(f -> f.getRating() == s).count());
        }

        return new EventReport(id, event.getTitle(), event.getStatus(), event.getCapacity(),
                confirmed, waitlisted, cancelled,
                registrationRepository.countByEventIdAndPromotedFromWaitlistTrue(id),
                pct(confirmed, event.getCapacity()),
                attendees, pct(attendees, confirmed), Math.max(0, confirmed - attendees),
                sessions, feedback.size(),
                avg(feedback, Feedback::getRating), avg(feedback, Feedback::getContentRating),
                avg(feedback, Feedback::getOrganizationRating),
                pct(feedback.stream().filter(Feedback::isWouldRecommend).count(), feedback.size()),
                distribution, aiService.fromJson(event.getAiFeedbackSummary()));
    }

    private static Double avg(List<Feedback> list, java.util.function.ToIntFunction<Feedback> f) {
        return list.isEmpty() ? null : round(list.stream().mapToInt(f).average().orElse(0));
    }

    private static double pct(long part, long whole) {
        return whole == 0 ? 0 : round(part * 100.0 / whole);
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
