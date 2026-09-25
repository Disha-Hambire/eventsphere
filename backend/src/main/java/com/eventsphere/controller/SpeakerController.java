package com.eventsphere.controller;

import com.eventsphere.dto.SpeakerDtos.SpeakerDto;
import com.eventsphere.dto.SpeakerDtos.SpeakerRequest;
import com.eventsphere.service.SpeakerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/speakers")
@Tag(name = "Speakers", description = "Speaker directory shared by all organizers")
public class SpeakerController {

    private final SpeakerService speakerService;

    public SpeakerController(SpeakerService speakerService) {
        this.speakerService = speakerService;
    }

    @GetMapping
    @Operation(summary = "List speakers")
    public List<SpeakerDto> list() {
        return speakerService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Add a speaker")
    public SpeakerDto create(@Valid @RequestBody SpeakerRequest request) {
        return speakerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Update a speaker")
    public SpeakerDto update(@PathVariable Long id, @Valid @RequestBody SpeakerRequest request) {
        return speakerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Delete a speaker (only if not assigned to any session)")
    public void delete(@PathVariable Long id) {
        speakerService.delete(id);
    }
}
