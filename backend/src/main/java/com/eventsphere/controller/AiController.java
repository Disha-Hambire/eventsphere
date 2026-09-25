package com.eventsphere.controller;

import com.eventsphere.dto.AiDtos.AiStatus;
import com.eventsphere.dto.AiDtos.AiText;
import com.eventsphere.dto.AiDtos.DescriptionRequest;
import com.eventsphere.service.ai.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
@Tag(name = "AI", description = "Gemini powered helpers")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/status")
    @Operation(summary = "Is Gemini configured?")
    public AiStatus status() {
        return aiService.status();
    }

    @PostMapping("/event-description")
    @Operation(summary = "Generate a marketing description for an event")
    public AiText eventDescription(@Valid @RequestBody DescriptionRequest request) {
        return aiService.generateEventDescription(request);
    }
}
