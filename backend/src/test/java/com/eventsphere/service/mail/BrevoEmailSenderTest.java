package com.eventsphere.service.mail;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Verifies the exact HTTP call made to Brevo, without sending a real e-mail. */
class BrevoEmailSenderTest {

    @Test
    void postsTransactionalEmailToBrevo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.brevo.test/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.sender.email").value("me@gmail.com"))
                .andExpect(jsonPath("$.sender.name").value("EventSphere"))
                .andExpect(jsonPath("$.to[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.to[0].name").value("Riya"))
                .andExpect(jsonPath("$.subject").value("Your code: 123456"))
                .andExpect(jsonPath("$.textContent").value("Hello"))
                .andRespond(withSuccess("{\"messageId\":\"<abc@brevo>\"}", MediaType.APPLICATION_JSON));

        new BrevoEmailSender(builder, "https://api.brevo.test/v3", "test-key", "me@gmail.com", "EventSphere")
                .send("user@example.com", "Riya", "Your code: 123456", "Hello");

        server.verify();
    }

    @Test
    void splitsDisplayNameAndAddress() {
        assertThat(EmailConfig.splitFrom("EventSphere <me@gmail.com>")).containsExactly("EventSphere", "me@gmail.com");
        assertThat(EmailConfig.splitFrom("me@gmail.com")).containsExactly("EventSphere", "me@gmail.com");
        assertThat(EmailConfig.splitFrom("\"Campus Events\" <events@x.org>")).containsExactly("Campus Events", "events@x.org");
    }
}
