package com.orthodox.orthodoxapp.auth;

import java.io.Serializable;

/**
 * SignedInUser: who is signed in, as remembered in their session.
 *
 * Spring Security keeps this in the session between requests, so it holds
 * only the account id and email, never the password. Controllers receive it
 * with @AuthenticationPrincipal and load the full AppUser from the database.
 *
 * Whether someone signs in with a password or with Google, they end up with
 * the same SignedInUser, so the rest of the app doesn't care which they used.
 *
 * "Serializable" lets the session be saved to disk when DevTools restarts the
 * app, so you stay signed in while developing.
 */
public record SignedInUser(Long id, String email) implements Serializable {
}
