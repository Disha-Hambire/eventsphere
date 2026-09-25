package com.eventsphere.controller;

import com.eventsphere.dto.AttendanceDtos.AttendanceRow;
import com.eventsphere.dto.AttendanceDtos.CheckInResult;
import com.eventsphere.dto.SessionDtos.SessionDto;
import com.eventsphere.dto.SessionDtos.SessionRequest;
import com.eventsphere.security.CurrentUserService;
import com.eventsphere.service.AttendanceService;
import com.eventsphere.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
@Tag(name = "Sessions & attendance", description = "Edit agenda sessions and manage the per-session attendance sheet")
public class SessionController {

    private final SessionService sessionService;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUser;

    public SessionController(SessionService sessionService, AttendanceService attendanceService,
                             CurrentUserService currentUser) {
        this.sessionService = sessionService;
        this.attendanceService = attendanceService;
        this.currentUser = currentUser;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a session")
    public SessionDto update(@PathVariable Long id, @Valid @RequestBody SessionRequest request) {
        return sessionService.update(id, request, currentUser.get());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a session (only if no attendance recorded)")
    public void delete(@PathVariable Long id) {
        sessionService.delete(id, currentUser.get());
    }

    @GetMapping("/{id}/attendance")
    @Operation(summary = "Attendance sheet: all confirmed participants with present/absent")
    public List<AttendanceRow> attendance(@PathVariable Long id) {
        return attendanceService.sheet(id, currentUser.get());
    }

    @PutMapping("/{id}/attendance/{registrationId}")
    @Operation(summary = "Manually mark a participant present")
    public CheckInResult markPresent(@PathVariable Long id, @PathVariable Long registrationId) {
        return attendanceService.markPresent(id, registrationId, currentUser.get());
    }

    @DeleteMapping("/{id}/attendance/{registrationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Undo a check-in")
    public void markAbsent(@PathVariable Long id, @PathVariable Long registrationId) {
        attendanceService.markAbsent(id, registrationId, currentUser.get());
    }
}
