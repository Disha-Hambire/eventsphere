package com.eventsphere.dto;

import com.eventsphere.entity.EventStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record SessionStat(Long sessionId, String title, String speakerName, LocalDateTime startTime,
                              long attended, double attendanceRate) {
    }

    public record EventReport(
            Long eventId,
            String title,
            EventStatus status,
            int capacity,
            long confirmed,
            long waitlisted,
            long cancelled,
            long promotedFromWaitlist,
            double fillRate,
            long attendees,
            double attendanceRate,
            long noShows,
            List<SessionStat> sessions,
            long feedbackCount,
            Double averageRating,
            Double averageContentRating,
            Double averageOrganizationRating,
            double recommendRate,
            Map<Integer, Long> ratingDistribution,
            AiDtos.FeedbackSummary aiSummary) {
    }

    public record TrendPoint(LocalDate date, long count) {
    }

    public record CategoryCount(String category, long count) {
    }

    public record TopEvent(Long id, String title, long confirmed, int capacity, double fillRate) {
    }

    public record Dashboard(
            long totalEvents,
            long draftEvents,
            long upcomingEvents,
            long liveEvents,
            long completedEvents,
            long totalRegistrations,
            long waitlisted,
            long totalCheckIns,
            double averageAttendanceRate,
            Double averageRating,
            List<TrendPoint> registrationTrend,
            List<CategoryCount> eventsByCategory,
            List<TopEvent> topEvents,
            List<EventDtos.EventDto> upcoming) {
    }

    /** Numbers for the animated counters on the public landing page. */
    public record PublicStats(long events, long registrations, long checkIns, long speakers, Double averageRating) {
    }
}
