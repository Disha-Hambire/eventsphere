package com.eventsphere.controller;

import com.eventsphere.dto.EventDtos.EventDetailDto;
import com.eventsphere.dto.EventDtos.EventDto;
import com.eventsphere.dto.EventDtos.EventRequest;
import com.eventsphere.dto.SessionDtos.SessionDto;
import com.eventsphere.dto.SessionDtos.SessionRequest;
import com.eventsphere.entity.EventCategory;
import com.eventsphere.entity.EventStatus;
import com.eventsphere.security.CurrentUserService;
import com.eventsphere.service.EventService;
import com.eventsphere.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Event lifecycle: draft, publish, complete, cancel; agenda sessions")
public class EventController {

    private final EventService eventService;
    private final SessionService sessionService;
    private final CurrentUserService currentUser;

    public EventController(EventService eventService, SessionService sessionService, CurrentUserService currentUser) {
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "List events visible to the current user (filter by status, category, text)")
    public List<EventDto> list(@RequestParam(required = false) EventStatus status,
                               @RequestParam(required = false) EventCategory category,
                               @RequestParam(required = false, name = "q") String query) {
        return eventService.list(currentUser.get(), status, category, query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Event details with agenda and the caller's registration")
    public EventDetailDto get(@PathVariable Long id) {
        return eventService.detail(id, currentUser.get());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Create an event (starts as DRAFT)")
    public EventDto create(@Valid @RequestBody EventRequest request) {
        return eventService.create(request, currentUser.get());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Update an event (capacity increase auto-promotes the waitlist)")
    public EventDto update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request, currentUser.get());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Delete a draft event")
    public void delete(@PathVariable Long id) {
        eventService.delete(id, currentUser.get());
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Publish a draft (needs at least one session)")
    public EventDto publish(@PathVariable Long id) {
        return eventService.publish(id, currentUser.get());
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Mark an event as completed (opens feedback)")
    public EventDto complete(@PathVariable Long id) {
        return eventService.complete(id, currentUser.get());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Cancel an event")
    public EventDto cancel(@PathVariable Long id) {
        return eventService.cancel(id, currentUser.get());
    }

    // ------------------------------------------------------------------ sessions (agenda)

    @GetMapping("/{id}/sessions")
    @Operation(summary = "Agenda of an event")
    public List<SessionDto> sessions(@PathVariable Long id) {
        eventService.detail(id, currentUser.get()); // visibility check
        return sessionService.listForEvent(id);
    }

    @PostMapping("/{id}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Add a session (checks speaker and room clashes)")
    public SessionDto addSession(@PathVariable Long id, @Valid @RequestBody SessionRequest request) {
        return sessionService.create(id, request, currentUser.get());
    }
}
