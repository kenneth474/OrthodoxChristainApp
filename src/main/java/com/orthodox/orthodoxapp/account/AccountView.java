package com.orthodox.orthodoxapp.account;

/**
 * AccountView: what the frontend is allowed to see about the signed-in user.
 *
 * We never send the AppUser entity itself as JSON: it holds the password hash
 * and lockout counters. This record copies only the safe, useful fields.
 * (The same idea as LiturgicalDay vs OrthocalDay: a clean "output" format.)
 */
public record AccountView(
        String email,
        String displayName,
        String phoneNumber,
        boolean emailVerified,
        boolean phoneVerified,
        TwoFactorMethod twoFactorMethod,
        boolean hasPassword,
        boolean googleLinked,
        boolean appleLinked) {

    public static AccountView from(AppUser user) {
        return new AccountView(
                user.getEmail(),
                user.getDisplayName(),
                user.getPhoneNumber(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getTwoFactorMethod(),
                user.hasPassword(),
                user.getGoogleId() != null,
                user.getAppleId() != null);
    }
}
