package com.eventsphere.controller;

import com.eventsphere.dto.AttendanceDtos.CheckInRequest;
import com.eventsphere.dto.AttendanceDtos.CheckInResult;
import com.eventsphere.dto.RegistrationDtos.RegistrationDto;
import com.eventsphere.dto.RegistrationDtos.RegistrationResult;
import com.eventsphere.security.CurrentUserService;
import com.eventsphere.service.AttendanceService;
import com.eventsphere.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Registrations & check-in", description = "Smart registration, waitlist, QR tickets and QR check-in")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUser;

    public RegistrationController(RegistrationService registrationService, AttendanceService attendanceService,
                                  CurrentUserService currentUser) {
        this.registrationService = registrationService;
        this.attendanceService = attendanceService;
        this.currentUser = currentUser;
    }

    @PostMapping("/events/{eventId}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARTICIPANT')")
    @Operation(summary = "Register for an event (confirmed if seats are left, otherwise waitlisted)")
    public RegistrationResult register(@PathVariable Long eventId) {
        return registrationService.register(eventId, currentUser.get());
    }

    @GetMapping("/events/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "All registrations of an event")
    public List<RegistrationDto> forEvent(@PathVariable Long eventId) {
        return registrationService.forEvent(eventId, currentUser.get());
    }

    @GetMapping(value = "/events/{eventId}/registrations/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Download the registration list as CSV")
    public ResponseEntity<byte[]> export(@PathVariable Long eventId) {
        String csv = registrationService.exportCsv(eventId, currentUser.get());
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}; // lets Excel detect UTF-8
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registrations-event-" + eventId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(out);
    }

    @GetMapping("/registrations/my")
    @PreAuthorize("hasRole('PARTICIPANT')")
    @Operation(summary = "My registrations and tickets")
    public List<RegistrationDto> my() {
        return registrationService.myRegistrations(currentUser.get());
    }

    @GetMapping("/registrations/{id}")
    @Operation(summary = "One registration with its timeline (owner or event organizer)")
    public RegistrationDto get(@PathVariable Long id) {
        return registrationService.get(id, currentUser.get());
    }

    @PostMapping("/registrations/{id}/cancel")
    @Operation(summary = "Cancel a registration (auto-promotes the next waitlisted participant)")
    public RegistrationResult cancel(@PathVariable Long id) {
        return registrationService.cancel(id, currentUser.get());
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary = "Check in a ticket (QR scan or typed code) to a session")
    public CheckInResult checkIn(@Valid @RequestBody CheckInRequest request) {
        return attendanceService.checkInByTicket(request, currentUser.get());
    }
}
