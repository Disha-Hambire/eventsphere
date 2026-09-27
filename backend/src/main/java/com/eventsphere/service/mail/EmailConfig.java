package com.eventsphere.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chooses how e-mail is delivered, in this order:
 * <ol>
 *   <li>BREVO_API_KEY set: Brevo HTTPS API (use this on Render's free plan, which blocks SMTP ports)</li>
 *   <li>MAIL_HOST set: SMTP (e.g. Gmail with an app password; fine on a laptop)</li>
 *   <li>neither: no e-mail; password-reset codes are shown on screen in demo mode</li>
 * </ol>
 */
@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);
    private static final Pattern NAME_AND_ADDRESS = Pattern.compile("^\\s*(.*?)\\s*<\\s*([^>]+)\\s*>\\s*$");

    @Bean
    public EmailDelivery emailDelivery(@Value("${app.mail.brevo.api-key:}") String brevoKey,
                                         @Value("${app.mail.brevo.base-url:https://api.brevo.com/v3}") String brevoUrl,
                                         @Value("${spring.mail.host:}") String smtpHost,
                                         @Value("${app.mail.from}") String from,
                                         ObjectProvider<JavaMailSender> javaMailSender,
                                         RestClient.Builder restClientBuilder) {
        if (!brevoKey.isBlank()) {
            String[] parts = splitFrom(from);
            log.info("E-mail delivery: Brevo API, sender {}", parts[1]);
            return new EmailDelivery(new BrevoEmailSender(restClientBuilder, brevoUrl, brevoKey.trim(), parts[1], parts[0]));
        }
        if (!smtpHost.isBlank()) {
            log.info("E-mail delivery: SMTP via {}", smtpHost);
            return new EmailDelivery(new SmtpEmailSender(javaMailSender.getObject(), smtpHost, from));
        }
        log.info("E-mail delivery: not configured (password-reset demo mode shows codes on screen)");
        return new EmailDelivery(null);
    }

    /** "EventSphere <me@x.com>" -> ["EventSphere", "me@x.com"]; "me@x.com" -> ["EventSphere", "me@x.com"]. */
    static String[] splitFrom(String from) {
        Matcher m = NAME_AND_ADDRESS.matcher(from == null ? "" : from);
        if (m.matches()) {
            String name = m.group(1).isBlank() ? "EventSphere" : m.group(1).replace("\"", "");
            return new String[]{name, m.group(2).trim()};
        }
        return new String[]{"EventSphere", from == null ? "" : from.trim()};
    }
}
