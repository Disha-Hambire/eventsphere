package com.eventsphere.service.mail;

/**
 * Sends a plain-text e-mail. Implementations: Brevo HTTP API (works on hosts that block SMTP ports, such as
 * Render's free plan) and classic SMTP (e.g. Gmail with an app password). {@link EmailConfig} picks one.
 */
public interface EmailSender {

    /** Human-readable name for logs, e.g. "Brevo API" or "SMTP smtp.gmail.com". */
    String name();

    void send(String toEmail, String toName, String subject, String text);
}
