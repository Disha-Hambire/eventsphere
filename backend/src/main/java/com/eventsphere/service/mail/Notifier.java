package com.eventsphere.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Best-effort notification e-mails. If e-mail is not configured, or sending fails, the business action still
 * succeeds; the log records what happened.
 */
@Component
public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    private final EmailDelivery delivery;

    public Notifier(EmailDelivery delivery) {
        this.delivery = delivery;
    }

    public void send(String toEmail, String toName, String subject, String text) {
        if (!delivery.isConfigured()) {
            log.info("E-mail not configured; skipped notification '{}' to {}", subject, toEmail);
            return;
        }
        try {
            delivery.sender().send(toEmail, toName, subject, text);
        } catch (Exception ex) {
            log.warn("Could not send notification '{}' to {}: {}", subject, toEmail, ex.getMessage());
        }
    }
}
