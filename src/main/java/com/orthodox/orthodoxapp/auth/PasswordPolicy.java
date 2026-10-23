package com.orthodox.orthodoxapp.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * PasswordPolicy: the rules every new password must follow.
 *
 * Long passwords with a mix of characters are much harder to guess. A new
 * password must:
 *   - be 12 to 128 characters long
 *   - contain an uppercase letter, a lowercase letter, a number and a symbol
 *   - not contain the name part of your email (kenneth@... -> "kenneth")
 *   - not be a common word dressed up with numbers, like "Password2026!"
 *
 * check(...) returns EVERY broken rule at once, so the sign-up page can list
 * them all instead of making people fix one at a time. The sign-in screen
 * (frontend/src/pages/SignInPage.tsx) shows the first five rules as a live
 * checklist while you type; this class is the final word, because a browser
 * can be tricked but the server can't.
 *
 * "final" class + private constructor = a toolbox of static methods that's
 * never turned into objects (like java.lang.Math).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    // Words that make a password easy to guess, even with numbers and symbols added.
    // Attackers try these (and words about the app itself) first.
    private static final Set<String> COMMON_WORDS = Set.of(
            "password", "passw", "qwerty", "qwertyuiop", "asdf", "letmein", "welcome",
            "admin", "login", "iloveyou", "monkey", "dragon", "football", "baseball",
            "sunshine", "princess", "abc", "abcdef", "trustno", "changeme",
            "orthodox", "orthodoxdaily", "antiochian", "church", "christ", "jesus",
            "jesuschrist", "god", "amen");

    private PasswordPolicy() {
    }

    /**
     * Checks a new password.
     *
     * @param password the password the person typed
     * @param email    their email, so we can reject passwords built from it
     * @return a friendly message for each broken rule; empty means the password is fine
     */
    public static List<String> check(String password, String email) {
        String pw = password == null ? "" : password;
        List<String> problems = new ArrayList<>();

        if (pw.length() < MIN_LENGTH) {
            problems.add("Use at least " + MIN_LENGTH + " characters.");
        }
        if (pw.length() > MAX_LENGTH) {
            problems.add("Use no more than " + MAX_LENGTH + " characters.");
        }
        // chars() turns the text into a stream of characters; noneMatch is true
        // when not a single character passes the test.
        if (pw.chars().noneMatch(Character::isUpperCase)) {
            problems.add("Add an uppercase letter (A-Z).");
        }
        if (pw.chars().noneMatch(Character::isLowerCase)) {
            problems.add("Add a lowercase letter (a-z).");
        }
        if (pw.chars().noneMatch(Character::isDigit)) {
            problems.add("Add a number (0-9).");
        }
        if (pw.chars().noneMatch(PasswordPolicy::isSymbol)) {
            problems.add("Add a symbol, such as ! ? # $ % or &.");
        }

        String lower = pw.toLowerCase(Locale.ROOT);
        String emailName = emailName(email);
        if (emailName.length() >= 3 && lower.contains(emailName)) {
            problems.add("Don't use your email name in your password.");
        }
        // "Password2026!" -> "password": a common word with decorations added.
        String lettersOnly = lower.replaceAll("[^a-z]", "");
        if (COMMON_WORDS.contains(lettersOnly)) {
            problems.add("Avoid common words like \"password\" or \"welcome\", even with numbers added.");
        }
        return problems;
    }

    // A symbol is anything that isn't a letter, a digit or a space: ! ? # $ % & ...
    private static boolean isSymbol(int c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
    }

    // "Kenneth.Kharel@gmail.com" -> "kenneth.kharel"
    private static String emailName(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);
    }
}
