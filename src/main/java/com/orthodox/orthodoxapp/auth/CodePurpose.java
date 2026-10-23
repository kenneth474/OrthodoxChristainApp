package com.orthodox.orthodoxapp.auth;

/**
 * CodePurpose: what a 6-digit verification code is for.
 *
 * Each purpose keeps its own codes, so a code sent to verify your phone
 * can't be used to finish signing in, and the other way round.
 */
public enum CodePurpose {
    SIGN_IN,        // the second step of signing in (two-step verification)
    VERIFY_EMAIL,   // proving you own your email address
    VERIFY_PHONE    // proving you own your phone number
}
