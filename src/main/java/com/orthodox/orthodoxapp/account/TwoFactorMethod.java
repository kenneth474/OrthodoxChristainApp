package com.orthodox.orthodoxapp.account;

/**
 * TwoFactorMethod: where a user's second sign-in step gets its code.
 *
 * Two-step verification (also called 2FA, "two-factor authentication") means
 * signing in needs two things: something you KNOW (your password) and
 * something you HAVE (your phone or your email inbox). Someone who steals
 * your password still can't get in without the code.
 */
public enum TwoFactorMethod {
    NONE,           // password only
    EMAIL,          // a 6-digit code sent to your verified email
    SMS,            // a 6-digit code sent by text message to your verified phone
    AUTHENTICATOR   // a 6-digit code from an authenticator app (see AuthenticatorService)
}
