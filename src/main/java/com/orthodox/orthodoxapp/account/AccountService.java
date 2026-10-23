package com.orthodox.orthodoxapp.account;

import com.orthodox.orthodoxapp.auth.AuthException;
import com.orthodox.orthodoxapp.auth.CodePurpose;
import com.orthodox.orthodoxapp.auth.PhoneNumbers;
import com.orthodox.orthodoxapp.auth.VerificationCodeService;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AccountService: the rules for changing your own account: your name, your
 * phone number, verifying your email and phone, and two-step verification.
 */
@Service
public class AccountService {

    private final AppUserRepository users;
    private final VerificationCodeService codes;

    public AccountService(AppUserRepository users, VerificationCodeService codes) {
        this.users = users;
        this.codes = codes;
    }

    /** Loads an account. 401 if it no longer exists (e.g. the database was reset). */
    public AppUser get(Long userId) {
        if (userId == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        }
        return users.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    @Transactional
    public AppUser updateName(Long userId, String displayName) {
        String name = displayName == null ? "" : displayName.strip();
        if (name.isEmpty() || name.length() > 60) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Enter your name (up to 60 characters).");
        }
        AppUser user = get(userId);
        user.setDisplayName(name);
        return users.save(user);
    }

    /**
     * Adds, changes or removes (blank) the phone number. A new number has to be
     * verified again, so we text it a code straight away.
     *
     * @return where the code went, or null if no code was needed
     */
    @Transactional
    public String updatePhone(Long userId, String phone) {
        AppUser user = get(userId);
        String newPhone = (phone == null || phone.isBlank()) ? null : PhoneNumbers.normalize(phone);
        if (Objects.equals(newPhone, user.getPhoneNumber())) {
            return null; // nothing changed
        }
        if (user.getTwoFactorMethod() == TwoFactorMethod.SMS) {
            throw new AuthException(HttpStatus.CONFLICT,
                    "Turn off text-message verification before changing or removing your number.");
        }
        user.setPhoneNumber(newPhone);
        user.setPhoneVerified(false);
        users.save(user);
        return newPhone == null ? null : codes.send(user, CodePurpose.VERIFY_PHONE);
    }

    public String sendEmailCode(Long userId) {
        AppUser user = get(userId);
        if (user.isEmailVerified()) {
            throw new AuthException(HttpStatus.CONFLICT, "Your email is already verified.");
        }
        return codes.send(user, CodePurpose.VERIFY_EMAIL);
    }

    /** noRollbackFor: keep the wrong-guess count (see VerificationCodeService.verify). */
    @Transactional(noRollbackFor = AuthException.class)
    public AppUser verifyEmail(Long userId, String code) {
        AppUser user = get(userId);
        codes.verify(user, CodePurpose.VERIFY_EMAIL, code);
        user.setEmailVerified(true);
        return users.save(user);
    }

    public String sendPhoneCode(Long userId) {
        AppUser user = get(userId);
        if (user.getPhoneNumber() == null) {
            throw new AuthException(HttpStatus.CONFLICT, "Add a phone number first.");
        }
        if (user.isPhoneVerified()) {
            throw new AuthException(HttpStatus.CONFLICT, "Your phone number is already verified.");
        }
        return codes.send(user, CodePurpose.VERIFY_PHONE);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AppUser verifyPhone(Long userId, String code) {
        AppUser user = get(userId);
        codes.verify(user, CodePurpose.VERIFY_PHONE, code);
        user.setPhoneVerified(true);
        return users.save(user);
    }

    /**
     * Turns two-step verification off, or on by email or text. Codes can only
     * go somewhere you've proven is yours, or you could lock yourself out.
     */
    @Transactional
    public AppUser setTwoFactor(Long userId, TwoFactorMethod method) {
        AppUser user = get(userId);
        TwoFactorMethod chosen = method == null ? TwoFactorMethod.NONE : method;

        if (chosen != TwoFactorMethod.NONE && !user.hasPassword()) {
            throw new AuthException(HttpStatus.CONFLICT, "Two-step verification protects password sign-in. "
                    + "You sign in with Google or Apple, which protect your account with their own security.");
        }
        if (chosen == TwoFactorMethod.EMAIL && !user.isEmailVerified()) {
            throw new AuthException(HttpStatus.CONFLICT, "Verify your email address first.");
        }
        if (chosen == TwoFactorMethod.SMS && !user.isPhoneVerified()) {
            throw new AuthException(HttpStatus.CONFLICT, "Add and verify your phone number first.");
        }
        user.setTwoFactorMethod(chosen);
        return users.save(user);
    }
}
