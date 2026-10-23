package com.orthodox.orthodoxapp.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConsoleEmailSender: development mode. Instead of sending the email, it
 * prints it in the terminal where the app is running, so you can copy the
 * code from there. Used until an email server is set in secrets.properties.
 */
public class ConsoleEmailSender implements EmailSender {

    // A logger writes messages to the terminal, with the time and class name.
    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        // """ starts a "text block": a string that spans several lines.
        // Each {} is filled in with the next value after the text.
        log.info("""

                ======== EMAIL (development mode: printed here, not sent) ========
                To:      {}
                Subject: {}
                {}
                ==================================================================""", to, subject, body);
    }
}
