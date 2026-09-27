package com.eventsphere.service;

import com.eventsphere.entity.PasswordResetToken;
import com.eventsphere.entity.User;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.repository.PasswordResetTokenRepository;
import com.eventsphere.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eventsphere.service.mail.EmailDelivery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Forgot-password with an e-mailed one-time code:
 * <ol>
 *   <li>Request: always answers the same way (no hint whether the e-mail exists). A random 6-digit code is created,
 *       stored only as a BCrypt hash, valid for 10 minutes; any older open code is invalidated.</li>
 *   <li>Delivery: e-mail via Brevo or SMTP when configured (see {@link com.eventsphere.service.mail.EmailConfig});
 *       otherwise demo mode returns the code so it can be shown on screen.</li>
 *   <li>Reset: e-mail + code + new password. Max 5 wrong attempts per code, then a new code is required.
 *       The code is burnt on success.</li>
 * </ol>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    static final int MAX_ATTEMPTS = 5;
    public static final String GENERIC_MESSAGE =
            "If an account exists for that email, we've sent a 6-digit code to it. The code expires in %d minutes.";
    private static final String INVALID = "The code is invalid or has expired. Please request a new one.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailDelivery emailDelivery;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    private final int expiryMinutes;
    private final boolean demoMode;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder, EmailDelivery emailDelivery, Clock clock,
                                @Value("${app.password-reset.expiry-minutes}") int expiryMinutes,
                                @Value("${app.password-reset.demo-mode}") boolean demoMode) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailDelivery = emailDelivery;
        this.clock = clock;
        this.expiryMinutes = expiryMinutes;
        this.demoMode = demoMode;
    }

    /** @param message  what to show the user (identical whether or not the account exists)
     *  @param demoCode the code, only when no mail server is configured and demo mode is on */
    public record ResetRequestResult(String message, String demoCode) {
    }

    @Transactional
    public ResetRequestResult requestReset(String email) {
        String message = GENERIC_MESSAGE.formatted(expiryMinutes);
        Optional<User> found = userRepository.findByEmailIgnoreCase(email.trim()).filter(User::isActive);
        if (found.isEmpty()) {
            log.info("Password reset requested for unknown or inactive email");
            return new ResetRequestResult(message, null);
        }
        User user = found.get();
        LocalDateTime now = LocalDateTime.now(clock);
        // Only the newest code is valid
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId()).forEach(t -> t.markUsed(now));

        String code = "%06d".formatted(random.nextInt(1_000_000));
        tokenRepository.save(new PasswordResetToken(user, passwordEncoder.encode(code), now, now.plusMinutes(expiryMinutes)));

        if (emailDelivery.isConfigured()) {
            sendEmail(user, code);
            return new ResetRequestResult(message, null);
        }
        log.warn("No e-mail delivery configured - password reset code for {}: {}", user.getEmail(), code);
        return new ResetRequestResult(message, demoMode ? code : null);
    }

    /**
     * Wrong guesses must be counted even though we answer with an error, so this transaction does not roll back
     * on business-rule failures.
     */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void resetPassword(String email, String code, String newPassword) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessRuleException(INVALID));
        PasswordResetToken token = tokenRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .filter(t -> !t.isExpired(now))
                .orElseThrow(() -> new BusinessRuleException(INVALID));
        if (token.isLocked(MAX_ATTEMPTS)) {
            throw new BusinessRuleException("Too many wrong attempts. Please request a new code.");
        }
        if (code == null || !passwordEncoder.matches(code.trim(), token.getTokenHash())) {
            token.registerFailedAttempt();
            int left = MAX_ATTEMPTS - token.getAttempts();
            throw new BusinessRuleException(left > 0
                    ? "Incorrect code. " + left + " attempt" + (left == 1 ? "" : "s") + " left."
                    : "Too many wrong attempts. Please request a new code.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.markUsed(now);
        log.info("Password reset completed for user {}", user.getId());
    }

    public boolean isMailConfigured() {
        return emailDelivery.isConfigured();
    }

    private void sendEmail(User user, String code) {
        String subject = "Your EventSphere password reset code: " + code;
        String text = "Hi " + user.getFullName() + ",\n\n"
                + "Use this code to reset your EventSphere password:\n\n"
                + "    " + code + "\n\n"
                + "It expires in " + expiryMinutes + " minutes and can be used once.\n"
                + "If you didn't ask for this, you can ignore this email; your password stays the same.\n\n- EventSphere";
        String channel = emailDelivery.sender().name();
        try {
            emailDelivery.sender().send(user.getEmail(), user.getFullName(), subject, text);
            log.info("Password reset code e-mailed to user {} via {}", user.getId(), channel);
        } catch (Exception ex) {
            // Don't reveal delivery problems to the requester; the log tells the operator what went wrong.
            log.error("Could not send password reset email to user {} via {}: {}", user.getId(), channel, ex.getMessage());
        }
    }
}
