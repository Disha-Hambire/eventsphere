package com.eventsphere.controller;

import com.eventsphere.dto.AuthDtos.AuthResponse;
import com.eventsphere.dto.AuthDtos.ForgotPasswordRequest;
import com.eventsphere.dto.AuthDtos.ForgotPasswordResponse;
import com.eventsphere.dto.AuthDtos.LoginRequest;
import com.eventsphere.dto.AuthDtos.MessageResponse;
import com.eventsphere.dto.AuthDtos.RegisterRequest;
import com.eventsphere.dto.AuthDtos.ResetPasswordRequest;
import com.eventsphere.dto.AuthDtos.UserDto;
import com.eventsphere.security.CurrentUserService;
import com.eventsphere.service.AuthService;
import com.eventsphere.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Sign up, log in and fetch the current user")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final CurrentUserService currentUser;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          CurrentUserService currentUser) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sign up as a participant")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "E-mail a 6-digit reset code (same answer whether or not the email exists)")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        PasswordResetService.ResetRequestResult result = passwordResetService.requestReset(request.email());
        return new ForgotPasswordResponse(result.message(), result.demoCode());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using the e-mailed code")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.email(), request.code(), request.newPassword());
        return new MessageResponse("Your password has been changed. You can now log in.");
    }

    @GetMapping("/me")
    @Operation(summary = "The logged-in user")
    public UserDto me() {
        return UserDto.from(currentUser.get());
    }
}
