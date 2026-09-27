package com.eventsphere.service.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Classic SMTP, e.g. Gmail with an app password. Not usable where SMTP ports are blocked (Render free plan). */
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String host;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, String host, String from) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
    }

    @Override
    public String name() {
        return "SMTP " + host;
    }

    @Override
    public void send(String toEmail, String toName, String subject, String text) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(toEmail);
        mail.setSubject(subject);
        mail.setText(text);
        mailSender.send(mail);
    }
}
