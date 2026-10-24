package com.orthodox.orthodoxapp.auth;

import com.orthodox.orthodoxapp.account.AppUser;
import com.orthodox.orthodoxapp.account.AppUserRepository;
import com.orthodox.orthodoxapp.account.TwoFactorMethod;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthenticatorService: two-step verification with an authenticator app.
 *
 * SETTING IT UP (Profile > Two-step verification)
 *   1. startSetup: we make a new secret and show it as a QR code. It's only
 *      kept in your session until you prove the app has it.
 *   2. confirmSetup: you type the 6-digit code your app now shows. If it's
 *      right, the secret is saved to your account, the app is switched on,
 *      and you get 10 one-time backup codes (shown only once).
 *
 * SIGNING IN
 *   After your password, verifySignIn checks the app's current code (see
 *   Totp), or one of your backup codes. Safety rules:
 *     - each code works only once, even within its 30 seconds ("replay")
 *     - 10 wrong codes lock the account for 15 minutes, so nobody can try
 *       all million codes (a correct PASSWORD doesn't reset this count)
 */
@Service
public class AuthenticatorService {

    static final int BACKUP_CODE_COUNT = 10;
    static final int MAX_WRONG_CODES = 10;
    static final Duration LOCK_TIME = Duration.ofMinutes(15);
    private static final String SETUP_ATTRIBUTE = "AUTHENTICATOR_SETUP";
    // The letters and digits of backup codes: no 0/O or 1/I, which are easy to mix up.
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** What the setup screen shows: the secret (to type in) and the QR code's contents. */
    public record Setup(String secret, String uri) {
    }

    private final SecureRandom random = new SecureRandom();
    private final AppUserRepository users;
    private final BackupCodeRepository backupCodes;

    public AuthenticatorService(AppUserRepository users, BackupCodeRepository backupCodes) {
        this.users = users;
        this.backupCodes = backupCodes;
    }

    /** Step 1 of setting up: a new secret, kept in this session until confirmed. */
    public Setup startSetup(AppUser user, HttpSession session) {
        if (!user.hasPassword()) {
            throw new AuthException(HttpStatus.CONFLICT, "Two-step verification protects password sign-in. "
                    + "You sign in with Google, which protects your account with its own security.");
        }
        String secret = Totp.newSecret();
        session.setAttribute(SETUP_ATTRIBUTE, secret);
        return new Setup(secret, Totp.setupUri(secret, user.getEmail()));
    }

    /**
     * Step 2: the code from the app proves it has the secret. Then the app is
     * switched on for this account.
     *
     * @return the 10 new backup codes, to show once
     */
    @Transactional
    public List<String> confirmSetup(AppUser user, HttpSession session, String code) {
        if (!(session.getAttribute(SETUP_ATTRIBUTE) instanceof String secret)) {
            throw new AuthException(HttpStatus.CONFLICT, "Your setup timed out. Please start again.");
        }
        OptionalLong step = Totp.verify(secret, code, Instant.now());
        if (step.isEmpty()) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "That code isn't right. Type the code your app shows now (it changes every 30 seconds).");
        }
        session.removeAttribute(SETUP_ATTRIBUTE);
        user.setTotpSecret(secret);
        user.setTotpLastStep(step.getAsLong());
        user.setTwoFactorMethod(TwoFactorMethod.AUTHENTICATOR);
        users.save(user);
        return newBackupCodes(user);
    }

    /** Makes 10 fresh backup codes (the old ones stop working) and returns them, e.g. "K7Q2M-9XHPA". */
    @Transactional
    public List<String> newBackupCodes(AppUser user) {
        if (user.getTwoFactorMethod() != TwoFactorMethod.AUTHENTICATOR) {
            throw new AuthException(HttpStatus.CONFLICT, "Turn on the authenticator app first.");
        }
        backupCodes.deleteByUserId(user.getId());
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 10; j++) {
                if (j == 5) {
                    code.append('-');
                }
                code.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
            }
            codes.add(code.toString());
            backupCodes.save(new BackupCode(user.getId(), hash(code.toString())));
        }
        return codes;
    }

    /** How many unused backup codes an account has. */
    public long backupCodesLeft(AppUser user) {
        return backupCodes.countByUserIdAndUsedFalse(user.getId());
    }

    /**
     * The second step of signing in: the app's code, or a backup code.
     * Returns normally if it's right.
     *
     * noRollbackFor: keep the wrong-code count even though we throw.
     */
    @Transactional(noRollbackFor = AuthException.class)
    public void verifySignIn(AppUser user, String typed) {
        Instant now = Instant.now();
        if (user.isLocked(now)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong codes. Please wait 15 minutes and try again.");
        }

        OptionalLong step = Totp.verify(user.getTotpSecret(), typed, now);
        boolean fresh = step.isPresent() && (user.getTotpLastStep() == null || step.getAsLong() > user.getTotpLastStep());
        if (fresh) {
            user.setTotpLastStep(step.getAsLong());
            user.clearFailedCodes();
            users.save(user);
            return;
        }
        if (step.isEmpty() && useBackupCode(user, typed)) {
            user.clearFailedCodes();
            users.save(user);
            return;
        }

        user.recordFailedCode(now, MAX_WRONG_CODES, LOCK_TIME);
        users.save(user);
        throw new AuthException(HttpStatus.BAD_REQUEST, step.isPresent()
                ? "That code was just used. Wait for your app to show the next one."
                : "That code isn't right. Check your authenticator app and try again.");
    }

    /** Switches the app off: forgets the secret and the backup codes. */
    @Transactional
    public void turnOff(AppUser user) {
        user.setTotpSecret(null);
        user.setTotpLastStep(null);
        backupCodes.deleteByUserId(user.getId());
    }

    private boolean useBackupCode(AppUser user, String typed) {
        if (typed == null || typed.isBlank()) {
            return false;
        }
        return backupCodes.findFirstByUserIdAndCodeHashAndUsedFalse(user.getId(), hash(typed))
                .map(code -> {
                    code.markUsed();
                    backupCodes.save(code);
                    return true;
                })
                .orElse(false);
    }

    /**
     * SHA-256 of a backup code, ignoring case, spaces and dashes. (Passwords
     * need slow BCrypt because people choose guessable ones; these codes are
     * random and long, about 50 bits, so a fast hash is safe.)
     */
    static String hash(String code) {
        String clean = code.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(clean.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is missing from this Java", e);
        }
    }
}
