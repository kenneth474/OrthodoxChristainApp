package com.orthodox.orthodoxapp.notify;

/**
 * SmsSender: sends one text message. Only the development version exists
 * (ConsoleSmsSender, which prints it): real codes by text go through Twilio
 * Verify instead (see TwilioVerify), which makes and sends its own codes.
 */
public interface SmsSender {

    /**
     * @param toPhone the number in E.164 format, e.g. +12145550123
     * @param body    the message text
     */
    void send(String toPhone, String body);

    /** True when texts are only printed in the terminal (see EmailSender). */
    default boolean developmentMode() {
        return false;
    }
}
