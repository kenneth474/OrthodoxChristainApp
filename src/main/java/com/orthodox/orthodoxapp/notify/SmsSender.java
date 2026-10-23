package com.orthodox.orthodoxapp.notify;

/**
 * SmsSender: sends one text message. Like EmailSender, it has a free
 * development version (ConsoleSmsSender) and a real one (TwilioSmsSender).
 */
public interface SmsSender {

    /**
     * @param toPhone the number in E.164 format, e.g. +12145550123
     * @param body    the message text
     */
    void send(String toPhone, String body);
}
