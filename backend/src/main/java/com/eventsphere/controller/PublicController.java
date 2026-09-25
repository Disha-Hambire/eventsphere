package com.eventsphere.controller;

import com.eventsphere.dto.EventDtos.EventDto;
import com.eventsphere.dto.ReportDtos.PublicStats;
import com.eventsphere.service.EventService;
import com.eventsphere.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "No login needed: landing page stats and featured events")
public class PublicController {

    private final ReportService reportService;
    private final EventService eventService;

    public PublicController(ReportService reportService, EventService eventService) {
        this.reportService = reportService;
        this.eventService = eventService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Platform totals for the landing page")
    public PublicStats stats() {
        return reportService.publicStats();
    }

    @GetMapping("/events")
    @Operation(summary = "Upcoming published events")
    public List<EventDto> featured() {
        return eventService.featured(6);
    }
}
