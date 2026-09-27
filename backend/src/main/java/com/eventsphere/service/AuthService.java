package com.eventsphere.service;

import com.eventsphere.config.AccountBootstrap;
import com.eventsphere.dto.AuthDtos.AuthResponse;
import com.eventsphere.dto.AuthDtos.LoginRequest;
import com.eventsphere.dto.AuthDtos.RegisterRequest;
import com.eventsphere.dto.AuthDtos.UserDto;
import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.exception.ApiException;
import com.eventsphere.exception.ConflictException;
import com.eventsphere.repository.UserRepository;
import com.eventsphere.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountBootstrap accounts;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AccountBootstrap accounts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accounts = accounts;
    }

    /**
     * Self sign-up creates a PARTICIPANT (organizer rights come from an approved organizer request), except for
     * addresses listed in ADMIN_EMAILS, which become the platform admins.
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists. Please log in.");
        }
        Role role = accounts.isAdminEmail(email) ? Role.ADMIN : Role.PARTICIPANT;
        User user = new User(req.fullName().trim(), email, passwordEncoder.encode(req.password()), role);
        user.setPhone(blankToNull(req.phone()));
        user.setOrganization(blankToNull(req.organization()));
        userRepository.save(user);
        return new AuthResponse(jwtService.issueToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Your account has been deactivated. Contact the admin.");
        }
        return new AuthResponse(jwtService.issueToken(user), UserDto.from(user));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
