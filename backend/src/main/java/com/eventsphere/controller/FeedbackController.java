package com.eventsphere.controller;

import com.eventsphere.dto.AiDtos.FeedbackSummary;
import com.eventsphere.dto.FeedbackDtos.FeedbackDto;
import com.eventsphere.dto.FeedbackDtos.FeedbackRequest;
import com.eventsphere.dto.ReportDtos.Dashboard;
import com.eventsphere.dto.ReportDtos.EventReport;
import com.eventsphere.security.CurrentUserService;
import com.eventsphere.service.FeedbackService;
import com.eventsphere.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Feedback & analytics", description = "Attendance-verified feedback, AI summaries, reports and dashboard")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final ReportService reportService;
    private final CurrentUserService currentUser;

    public FeedbackController(FeedbackService feedbackService, ReportService reportService,
                              CurrentUserService currentUser) {
        this.feedbackService = feedbackService;
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @PostMapping("/events/{eventId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARTICIPANT')")
    @Operation(summary = "Give feedback (only after the event is completed and only if you attended)")
    public FeedbackDto submit(@PathVariable Long eventId, @Valid @RequestBody FeedbackRequest request) {
        return feedbackService.submit(eventId, request, currentUser.get());
    }

    @GetMapping("/events/{eventId}/feedback")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "All feedback for an event")
    public List<FeedbackDto> forEvent(@PathVariable Long eventId) {
        return feedbackService.forEvent(eventId, currentUser.get());
    }

    @GetMapping("/events/{eventId}/report")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Event report: fill rate, attendance, no-shows, sessions, ratings")
    public EventReport report(@PathVariable Long eventId) {
        return reportService.eventReport(eventId, currentUser.get());
    }

    @PostMapping("/events/{eventId}/ai-summary")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Generate an AI summary of the event's feedback (Gemini, with rule-based fallback)")
    public FeedbackSummary aiSummary(@PathVariable Long eventId) {
        return reportService.generateAiSummary(eventId, currentUser.get());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Analytics dashboard (admin: all events, organizer: own events)")
    public Dashboard dashboard() {
        return reportService.dashboard(currentUser.get());
    }
}
