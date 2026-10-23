package com.orthodox.orthodoxapp.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConsoleSmsSender: development mode. Prints text messages in the terminal
 * instead of sending them. Used until Twilio is set in secrets.properties.
 */
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String toPhone, String body) {
        log.info("""

                ======== TEXT MESSAGE (development mode: printed here, not sent) ========
                To: {}
                {}
                =========================================================================""", toPhone, body);
    }

    @Override
    public boolean developmentMode() {
        return true;
    }
}
