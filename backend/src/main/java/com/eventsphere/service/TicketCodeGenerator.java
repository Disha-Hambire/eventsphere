package com.eventsphere.service;

import com.eventsphere.repository.RegistrationRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unguessable, human-readable ticket codes such as {@code ES-7KQ4-M9XD}.
 * Ambiguous characters (0/O, 1/I/L) are excluded so codes can also be typed in manually at the door.
 */
@Component
public class TicketCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final RegistrationRepository registrationRepository;

    public TicketCodeGenerator(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public String newCode() {
        String code;
        do {
            code = "ES-" + block() + "-" + block();
        } while (registrationRepository.existsByTicketCode(code));
        return code;
    }

    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase();
    }

    private String block() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
