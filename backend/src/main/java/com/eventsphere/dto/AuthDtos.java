package com.eventsphere.dto;

import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Full name is required") @Size(max = 120) String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
            @NotBlank(message = "Password is required") @Size(min = 6, max = 72, message = "Password must be 6-72 characters") String password,
            @Size(max = 30) String phone,
            @Size(max = 160) String organization) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record AuthResponse(String token, UserDto user) {
    }

    public record UserDto(Long id, String fullName, String email, Role role, String phone,
                          String organization, boolean active, LocalDateTime createdAt) {

        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getPhone(),
                    u.getOrganization(), u.isActive(), u.getCreatedAt());
        }
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email) {
    }

    /** demoCode is only present when no mail server is configured (local demo). */
    public record ForgotPasswordResponse(String message, String demoCode) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
            @NotBlank(message = "Enter the 6-digit code from the email")
            @Pattern(regexp = "\\s*\\d{6}\\s*", message = "The code has 6 digits") String code,
            @NotBlank(message = "New password is required") @Size(min = 6, max = 72, message = "Password must be 6-72 characters") String newPassword) {
    }

    public record MessageResponse(String message) {
    }

    public record UpdateRoleRequest(@NotNull(message = "Role is required") Role role) {
    }

    public record UpdateStatusRequest(@NotNull(message = "Active flag is required") Boolean active) {
    }
}
