package com.orthodox.orthodoxapp.auth;

/**
 * SentCode: where a verification code went, for the page to show.
 *
 * @param sentTo          partly hidden, e.g. "k•••@gmail.com" or "•••• 0123";
 *                        null when no code was sent
 * @param developmentMode true when the code was only printed in the terminal
 *                        running the app (no email/text settings yet), so the
 *                        page can tell you to look there
 */
public record SentCode(String sentTo, boolean developmentMode) {

    /** For actions that didn't need a code. */
    public static SentCode none() {
        return new SentCode(null, false);
    }
}
