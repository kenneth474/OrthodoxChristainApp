package com.orthodox.orthodoxapp.auth;

import com.orthodox.orthodoxapp.account.AppUser;
import com.orthodox.orthodoxapp.account.TwoFactorMethod;
import com.orthodox.orthodoxapp.notify.EmailSender;
import com.orthodox.orthodoxapp.notify.SmsSender;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VerificationCodeService: creates, sends and checks 6-digit codes.
 *
 * Used for three things (see CodePurpose): the second step of signing in, and
 * proving you own your email or phone. The safety rules:
 *   - codes come from SecureRandom, which can't be predicted
 *   - only a hash of the code is stored (like passwords)
 *   - a code expires after 10 minutes and allows 5 wrong guesses
 *   - asking for a new code retires the old one, and you must wait 30 seconds
 *     between requests, so nobody can flood an inbox or phone
 */
@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);

    static final Duration LIFETIME = Duration.ofMinutes(10);
    static final Duration RESEND_WAIT = Duration.ofSeconds(30);

    private final SecureRandom random = new SecureRandom();
    private final VerificationCodeRepository codes;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public VerificationCodeService(VerificationCodeRepository codes, PasswordEncoder passwordEncoder,
                                   EmailSender emailSender, SmsSender smsSender) {
        this.codes = codes;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    /**
     * Creates a new code and sends it by email or text, depending on the purpose.
     *
     * @Transactional: everything in this method happens as one unit in the
     * database. If sending fails and throws an exception, the new code is not
     * saved either (the change is "rolled back").
     *
     * @return where the code went (partly hidden, e.g. "k•••@gmail.com"), and
     *         whether it was only printed in the terminal
     */
    @Transactional
    public SentCode send(AppUser user, CodePurpose purpose) {
        Instant now = Instant.now();

        // Asked less than 30 seconds ago? Then wait.
        codes.findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(latest -> latest.getCreatedAt().isAfter(now.minus(RESEND_WAIT)))
                .ifPresent(latest -> {
                    throw new AuthException(HttpStatus.TOO_MANY_REQUESTS,
                            "Please wait 30 seconds before asking for another code.");
                });

        // Only the newest code should work, so retire any older ones.
        codes.findByUserIdAndPurposeAndUsedFalse(user.getId(), purpose).forEach(VerificationCode::retire);

        // A random number from 0 to 999999, padded to 6 digits: 42 -> "000042".
        String code = "%06d".formatted(random.nextInt(1_000_000));
        codes.save(new VerificationCode(user.getId(), purpose, passwordEncoder.encode(code), now, now.plus(LIFETIME)));

        return deliver(user, purpose, code);
    }

    /**
     * Checks a code the person typed. Returns normally if it's right, and
     * retires it so it can't be used twice.
     *
     * noRollbackFor: normally an exception undoes the database changes in a
     * @Transactional method. Here we WANT to keep the wrong-guess count even
     * though we throw an exception, or guesses would never add up.
     *
     * @throws AuthException (400) if the code is wrong, expired or used up
     */
    @Transactional(noRollbackFor = AuthException.class)
    public void verify(AppUser user, CodePurpose purpose, String submitted) {
        Instant now = Instant.now();
        VerificationCode code = codes.findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(c -> !c.isExpired(now))
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST,
                        "That code has expired or was already used. Ask for a new one."));

        String typed = submitted == null ? "" : submitted.strip();
        // passwordEncoder.matches compares the typed code with the stored hash.
        if (!typed.matches("\\d{6}") || !passwordEncoder.matches(typed, code.getCodeHash())) {
            code.recordWrongGuess();
            codes.save(code);
            throw new AuthException(HttpStatus.BAD_REQUEST, code.isUsed()
                    ? "Too many wrong tries. Ask for a new code."
                    : "That code isn't right. Check it and try again.");
        }

        code.retire();
        codes.save(code);
    }

    // Sends the code by text for phone checks and SMS two-step sign-in,
    // otherwise by email.
    private SentCode deliver(AppUser user, CodePurpose purpose, String code) {
        String message = "Your Orthodox Daily code is " + code + ". It expires in 10 minutes. "
                + "If you didn't ask for it, you can ignore this message.";
        boolean byText = purpose == CodePurpose.VERIFY_PHONE
                || (purpose == CodePurpose.SIGN_IN && user.getTwoFactorMethod() == TwoFactorMethod.SMS);
        try {
            if (byText) {
                smsSender.send(user.getPhoneNumber(), message);
                return new SentCode(PhoneNumbers.mask(user.getPhoneNumber()), smsSender.developmentMode());
            }
            emailSender.send(user.getEmail(), "Your Orthodox Daily code", message);
            return new SentCode(maskEmail(user.getEmail()), emailSender.developmentMode());
        } catch (RuntimeException ex) {
            // Log the technical details for us; give the person a friendly message.
            log.error("Could not send a {} code to user {}", purpose, user.getId(), ex);
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We couldn't send your code. Please try again in a moment.");
        }
    }

    // "kenneth@gmail.com" -> "k•••@gmail.com"
    static String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.charAt(0) + "•••" + email.substring(at);
    }
}
