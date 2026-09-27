package com.eventsphere.service.mail;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends through Brevo's transactional e-mail API over HTTPS (https://developers.brevo.com/reference/sendtransacemail).
 * The sender address must be verified in the Brevo account.
 */
public class BrevoEmailSender implements EmailSender {

    private final RestClient client;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailSender(RestClient.Builder builder, String baseUrl, String apiKey, String fromEmail, String fromName) {
        this.client = builder.baseUrl(baseUrl).defaultHeader("api-key", apiKey).build();
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public String name() {
        return "Brevo API";
    }

    @Override
    public void send(String toEmail, String toName, String subject, String text) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", List.of(Map.of("email", toEmail, "name", toName == null ? toEmail : toName)),
                "subject", subject,
                "textContent", text);
        client.post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
