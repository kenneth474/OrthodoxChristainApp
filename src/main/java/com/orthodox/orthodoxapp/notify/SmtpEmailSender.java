package com.orthodox.orthodoxapp.notify;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * SmtpEmailSender: really sends email through an SMTP server (the standard
 * way programs send email), such as Gmail's.
 *
 * Spring Boot creates the JavaMailSender for us from the spring.mail.*
 * settings in secrets.properties; we just fill in a message and hand it over.
 */
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
