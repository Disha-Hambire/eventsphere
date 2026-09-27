package com.eventsphere.controller;

import com.eventsphere.dto.OrganizerRequestDtos.DecisionRequest;
import com.eventsphere.dto.OrganizerRequestDtos.OrganizerRequestDto;
import com.eventsphere.dto.OrganizerRequestDtos.SubmitRequest;
import com.eventsphere.entity.OrganizerRequest;
import com.eventsphere.exception.ApiException;
import com.eventsphere.security.CurrentUserService;
import com.eventsphere.service.OrganizerRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Organizer requests", description = "Participants apply to become organizers; admins decide")
public class OrganizerRequestController {

    private final OrganizerRequestService service;
    private final CurrentUserService currentUser;

    public OrganizerRequestController(OrganizerRequestService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/organizer-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARTICIPANT')")
    @Operation(summary = "Apply to become an organizer")
    public OrganizerRequestDto submit(@Valid @RequestBody SubmitRequest request) {
        return service.submit(currentUser.get(), request.organization(), request.reason());
    }

    @GetMapping("/organizer-requests/my")
    @Operation(summary = "My latest organizer request (204 if none)")
    public ResponseEntity<OrganizerRequestDto> my() {
        OrganizerRequestDto dto = service.latestFor(currentUser.get());
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @GetMapping("/admin/organizer-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List organizer requests (status=PENDING by default; status=ALL for history)")
    public List<OrganizerRequestDto> list(@RequestParam(defaultValue = "PENDING") String status) {
        if ("ALL".equalsIgnoreCase(status)) {
            return service.list(null);
        }
        try {
            return service.list(OrganizerRequest.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status must be PENDING, APPROVED, REJECTED or ALL");
        }
    }

    @GetMapping("/admin/organizer-requests/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Number of pending requests (for the sidebar badge)")
    public Map<String, Long> pendingCount() {
        return Map.of("pending", service.pendingCount());
    }

    @PostMapping("/admin/organizer-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve: the applicant becomes an organizer")
    public OrganizerRequestDto approve(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest body) {
        return service.approve(id, currentUser.get(), body == null ? null : body.note());
    }

    @PostMapping("/admin/organizer-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject with an optional note to the applicant")
    public OrganizerRequestDto reject(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest body) {
        return service.reject(id, currentUser.get(), body == null ? null : body.note());
    }
}
