package com.orthodox.orthodoxapp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Checks each password rule on its own, so a failing test names the exact rule.
 */
class PasswordPolicyTest {

    private static final String EMAIL = "kenneth@example.com";

    @Test
    void aStrongPasswordPasses() {
        assertEquals(List.of(), PasswordPolicy.check("Vespers-at-6:30pm", EMAIL));
        assertEquals(List.of(), PasswordPolicy.check("Correct Horse 42!", EMAIL)); // spaces are fine
    }

    @Test
    void tooShort() {
        assertTrue(PasswordPolicy.check("Ab1!efgh", EMAIL).contains("Use at least 12 characters."));
    }

    @Test
    void tooLong() {
        String tooLong = "Aa1!".repeat(33) + "Aa"; // 134 characters
        assertTrue(PasswordPolicy.check(tooLong, EMAIL).contains("Use no more than 128 characters."));
    }

    @Test
    void needsEveryKindOfCharacter() {
        List<String> problems = PasswordPolicy.check("aaaaaaaaaaaaaaa", EMAIL);
        assertTrue(problems.contains("Add an uppercase letter (A-Z)."));
        assertTrue(problems.contains("Add a number (0-9)."));
        assertTrue(problems.contains("Add a symbol, such as ! ? # $ % or &."));

        assertTrue(PasswordPolicy.check("AAAAAAAAAA1!", EMAIL).contains("Add a lowercase letter (a-z)."));
    }

    @Test
    void mayNotContainTheEmailName() {
        assertTrue(PasswordPolicy.check("Kenneth-2026-rocks", EMAIL)
                .contains("Don't use your email name in your password."));
    }

    @Test
    void commonWordsWithDecorationsAreRejected() {
        String message = "Avoid common words like \"password\" or \"welcome\", even with numbers added.";
        assertTrue(PasswordPolicy.check("Password2026!!", EMAIL).contains(message));
        assertTrue(PasswordPolicy.check("Orthodox!2026#", EMAIL).contains(message));
    }

    @Test
    void reportsEveryProblemAtOnce() {
        // Empty password: too short, and missing all four kinds of character.
        assertEquals(5, PasswordPolicy.check("", EMAIL).size());
    }
}
