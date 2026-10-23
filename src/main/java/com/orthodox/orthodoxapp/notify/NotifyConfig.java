package com.orthodox.orthodoxapp.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * NotifyConfig: decides, when the app starts, whether emails and texts are
 * really sent or just printed in the terminal.
 *
 * @Configuration = "this class sets things up". Each @Bean method creates one
 * shared object that Spring hands to any class asking for that type. Here,
 * anything that asks for an EmailSender gets whichever one this method picks.
 */
@Configuration
@EnableConfigurationProperties(TwilioProperties.class)
public class NotifyConfig {

    private static final Logger log = LoggerFactory.getLogger(NotifyConfig.class);

    /**
     * Spring Boot only creates a JavaMailSender when spring.mail.host is set
     * (in secrets.properties). ObjectProvider lets us ask "is there one?"
     * without failing when there isn't.
     */
    @Bean
    EmailSender emailSender(ObjectProvider<JavaMailSender> mailSender,
                            @Value("${app.mail.from:${spring.mail.username:}}") String from) {
        JavaMailSender realMail = mailSender.getIfAvailable();
        if (realMail == null) {
            log.info("Email: development mode. Codes will be printed in this terminal.");
            return new ConsoleEmailSender();
        }
        log.info("Email: sending real emails from {}", from);
        return new SmtpEmailSender(realMail, from);
    }

    @Bean
    SmsSender smsSender(TwilioProperties twilio) {
        if (!twilio.configured()) {
            log.info("Text messages: development mode. Codes will be printed in this terminal.");
            return new ConsoleSmsSender();
        }
        log.info("Text messages: sending real texts through Twilio from {}", twilio.fromNumber());
        return new TwilioSmsSender(twilio);
    }
}
