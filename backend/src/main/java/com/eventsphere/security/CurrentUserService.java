package com.eventsphere.security;

import com.eventsphere.entity.User;
import com.eventsphere.exception.ApiException;
import com.eventsphere.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Resolves the logged-in user from the JWT. Loading from the DB (instead of trusting the token)
 * means a deactivated user or a changed role takes effect immediately.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in");
        }
        User user = userRepository.findById(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Your account no longer exists"));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Your account has been deactivated");
        }
        return user;
    }
}
