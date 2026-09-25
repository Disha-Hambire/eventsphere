package com.eventsphere.service;

import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.repository.PasswordResetTokenRepository;
import com.eventsphere.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetServiceTest extends IntegrationTest {

    @Autowired PasswordResetService resetService;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static String wrong(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    @DisplayName("Request a code, reset the password, and the code cannot be used twice")
    void fullResetFlow() {
        User user = user(Role.PARTICIPANT);
        String code = resetService.requestReset(user.getEmail().toUpperCase()).demoCode();

        assertThat(code).as("demo mode without SMTP shows the code").matches("\\d{6}");
        assertThat(tokenRepository.findAll()).noneMatch(t -> t.getTokenHash().equals(code))
                .as("only a hash is stored, never the code");

        resetService.resetPassword(user.getEmail(), " " + code + " ", "brand-new-pass");
        assertThat(passwordEncoder.matches("brand-new-pass",
                userRepository.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();

        assertThatThrownBy(() -> resetService.resetPassword(user.getEmail(), code, "another-pass"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    @DisplayName("Unknown emails get the same message and no code (no account enumeration)")
    void unknownEmailRevealsNothing() {
        User user = user(Role.PARTICIPANT);
        var known = resetService.requestReset(user.getEmail());
        var unknown = resetService.requestReset("nobody@nowhere.test");

        assertThat(unknown.message()).isEqualTo(known.message());
        assertThat(unknown.demoCode()).isNull();
    }

    @Test
    @DisplayName("Codes expire after 10 minutes")
    void expiredCodeRejected() {
        User user = user(Role.PARTICIPANT);
        String code = resetService.requestReset(user.getEmail()).demoCode();
        clock.set(NOW.plusMinutes(11));

        assertThatThrownBy(() -> resetService.resetPassword(user.getEmail(), code, "brand-new-pass"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Requesting a new code invalidates the previous one")
    void newRequestInvalidatesOldCode() {
        User user = user(Role.PARTICIPANT);
        String first = resetService.requestReset(user.getEmail()).demoCode();
        String second = resetService.requestReset(user.getEmail()).demoCode();

        if (!first.equals(second)) { // (1 in a million they are equal)
            assertThatThrownBy(() -> resetService.resetPassword(user.getEmail(), first, "brand-new-pass"))
                    .isInstanceOf(BusinessRuleException.class);
        }
        resetService.resetPassword(user.getEmail(), second, "brand-new-pass");
    }

    @Test
    @DisplayName("A code for one account does not work for another account")
    void codeIsBoundToItsAccount() {
        User alice = user(Role.PARTICIPANT);
        User bob = user(Role.PARTICIPANT);
        String aliceCode = resetService.requestReset(alice.getEmail()).demoCode();
        resetService.requestReset(bob.getEmail());

        assertThatThrownBy(() -> resetService.resetPassword(bob.getEmail(), aliceCode, "brand-new-pass"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("After 5 wrong attempts the code is locked, even the correct one")
    void lockedAfterFiveWrongAttempts() {
        User user = user(Role.PARTICIPANT);
        String code = resetService.requestReset(user.getEmail()).demoCode();

        for (int i = 1; i <= 4; i++) {
            assertThatThrownBy(() -> resetService.resetPassword(user.getEmail(), wrong(code), "brand-new-pass"))
                    .hasMessageContaining("Incorrect code");
        }
        assertThatThrownBy(() -> resetService.resetPassword(user.getEmail(), wrong(code), "brand-new-pass"))
                .hasMessageContaining("Too many wrong attempts");
        assertThatThrownBy(() -> resetService.resetPassword(user.getEmail(), code, "brand-new-pass"))
                .hasMessageContaining("Too many wrong attempts");
    }
}
