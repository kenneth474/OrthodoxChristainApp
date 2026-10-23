package com.orthodox.orthodoxapp.auth;

import com.orthodox.orthodoxapp.account.AppUser;
import com.orthodox.orthodoxapp.account.AppUserRepository;
import com.orthodox.orthodoxapp.account.TwoFactorMethod;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService: the business rules for creating accounts and signing in.
 *
 * AuthController handles the web requests; this class decides whether a
 * password is right, when to lock an account, and how a Google or Apple
 * sign-in connects to an account.
 */
@Service
public class AuthService {

    static final int MAX_FAILED_SIGN_INS = 5;
    static final Duration LOCK_TIME = Duration.ofMinutes(15);

    // Something@something.something, with no spaces. The real test of an
    // email address is sending it a code, which we do when verifying it.
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    // A hash of a random password, checked when the email has no account, so a
    // wrong email takes as long as a wrong password. Otherwise an attacker
    // could time our replies to learn which emails have accounts.
    private final String decoyHash;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.decoyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Creates an account with an email and password.
     *
     * @param phone optional; null or blank means none
     * @throws AuthException with a friendly message if anything is wrong
     */
    @Transactional
    public AppUser register(String email, String password, String displayName, String phone) {
        String cleanEmail = normalizeEmail(email);
        if (cleanEmail.length() > 254 || !EMAIL.matcher(cleanEmail).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        }

        String cleanName = displayName == null ? "" : displayName.strip();
        if (cleanName.isEmpty() || cleanName.length() > 60) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Enter your name (up to 60 characters).");
        }

        List<String> problems = PasswordPolicy.check(password, cleanEmail);
        if (!problems.isEmpty()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Please choose a stronger password.", problems);
        }

        String cleanPhone = (phone == null || phone.isBlank()) ? null : PhoneNumbers.normalize(phone);

        if (users.existsByEmail(cleanEmail)) {
            throw new AuthException(HttpStatus.CONFLICT,
                    "An account with this email already exists. Try signing in instead.");
        }

        AppUser user = new AppUser(cleanEmail, cleanName);
        user.setPasswordHash(passwordEncoder.encode(password)); // BCrypt: only the hash is saved
        user.setPhoneNumber(cleanPhone);
        return users.save(user);
    }

    /**
     * Checks an email and password. Returns the account if they're right.
     *
     * Every failure gives the SAME message, so an attacker can't learn whether
     * the email or the password was wrong. 5 wrong passwords in a row lock the
     * account for 15 minutes.
     */
    @Transactional(noRollbackFor = AuthException.class) // keep the failure count when we throw
    public AppUser checkPassword(String email, String password) {
        String typed = password == null ? "" : password;
        AppUser user = users.findByEmail(normalizeEmail(email)).orElse(null);

        if (user == null || !user.hasPassword()) {
            passwordEncoder.matches(typed, decoyHash); // same time as a real check
            throw wrongEmailOrPassword();
        }

        Instant now = Instant.now();
        if (user.isLocked(now)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong passwords. Please wait 15 minutes and try again.");
        }

        if (!passwordEncoder.matches(typed, user.getPasswordHash())) {
            user.recordFailedSignIn(now, MAX_FAILED_SIGN_INS, LOCK_TIME);
            users.save(user);
            throw wrongEmailOrPassword();
        }

        user.recordSuccessfulSignIn();
        return users.save(user);
    }

    /**
     * Finds or creates the account for someone who signed in with Google or Apple.
     *
     *   1. Already used this Google/Apple account here? Use the same AppUser.
     *   2. Otherwise, an account with the same (verified) email? Link to it.
     *   3. Otherwise, create a new account.
     *
     * @param provider      "google" or "apple"
     * @param subject       the provider's permanent ID for this person
     * @param emailVerified whether the provider has confirmed the email
     */
    @Transactional
    public AppUser signInWithProvider(String provider, String subject, String email,
                                      boolean emailVerified, String name) {
        Optional<AppUser> linked = switch (provider) {
            case "google" -> users.findByGoogleId(subject);
            case "apple" -> users.findByAppleId(subject);
            default -> throw new IllegalArgumentException("Unknown sign-in provider: " + provider);
        };
        if (linked.isPresent()) {
            return linked.get();
        }

        if (email == null || !emailVerified) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "Your " + provider + " account didn't share a verified email address.");
        }
        String cleanEmail = normalizeEmail(email);

        AppUser user = users.findByEmail(cleanEmail)
                .orElseGet(() -> new AppUser(cleanEmail, nameOrDefault(name, cleanEmail)));

        if (user.getId() != null && !user.isEmailVerified()) {
            // An existing account whose email was never proven. Anyone can sign
            // up with someone else's email and wait for the real owner to arrive
            // (a "pre-hijacking" attack). Google/Apple have now proven who owns
            // this email, so we remove the unproven password and settings.
            user.setPasswordHash(null);
            user.setPhoneNumber(null);
            user.setPhoneVerified(false);
            user.setTwoFactorMethod(TwoFactorMethod.NONE);
        }

        user.setEmailVerified(true);
        if (provider.equals("google")) {
            user.setGoogleId(subject);
        } else {
            user.setAppleId(subject);
        }
        return users.save(user);
    }

    // Emails are compared in lowercase without spaces: " Kenneth@Gmail.com" -> "kenneth@gmail.com"
    static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    // Apple doesn't share a name after the first sign-in, so fall back to the email.
    private static String nameOrDefault(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        return email.substring(0, email.indexOf('@'));
    }

    private static AuthException wrongEmailOrPassword() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
    }
}
