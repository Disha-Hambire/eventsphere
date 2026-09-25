package com.eventsphere.controller;

import com.eventsphere.dto.AuthDtos.UpdateRoleRequest;
import com.eventsphere.dto.AuthDtos.UpdateStatusRequest;
import com.eventsphere.dto.AuthDtos.UserDto;
import com.eventsphere.security.CurrentUserService;
import com.eventsphere.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User administration", description = "Admin only: promote organizers, deactivate accounts")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final CurrentUserService currentUser;

    public UserAdminController(UserAdminService userAdminService, CurrentUserService currentUser) {
        this.userAdminService = userAdminService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "List all users")
    public List<UserDto> list() {
        return userAdminService.list();
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change a user's role (e.g. make a participant an organizer)")
    public UserDto changeRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return userAdminService.changeRole(id, request.role(), currentUser.get());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user")
    public UserDto changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return userAdminService.changeStatus(id, request.active(), currentUser.get());
    }
}
