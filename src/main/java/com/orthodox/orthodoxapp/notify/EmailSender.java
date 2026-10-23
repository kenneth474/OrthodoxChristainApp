package com.orthodox.orthodoxapp.notify;

/**
 * EmailSender: sends one plain-text email.
 *
 * WHY AN INTERFACE?
 * An interface lists what something can DO, without saying HOW. Two classes
 * do the "how":
 *   - ConsoleEmailSender prints the email in the VS Code terminal (free, for development)
 *   - SmtpEmailSender really sends it through an email server
 * NotifyConfig picks one when the app starts. The rest of the app only knows
 * "EmailSender", so switching to real email needs no other code changes.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
