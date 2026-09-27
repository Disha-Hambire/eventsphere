package com.eventsphere.service.mail;

/**
 * The e-mail channel chosen at startup by {@link EmailConfig}. {@code sender} is null when e-mail is not configured.
 */
public record EmailDelivery(EmailSender sender) {

    public boolean isConfigured() {
        return sender != null;
    }
}
