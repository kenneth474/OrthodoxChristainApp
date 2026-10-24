package com.orthodox.orthodoxapp.auth;

import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * SocialClientRegistrations: the "Sign in with Google" setup.
 *
 * HOW "SIGN IN WITH GOOGLE" WORKS (OAuth 2.0 / OpenID Connect)
 *   1. You click "Continue with Google". We send your browser to Google
 *      (/oauth2/authorization/google -> accounts.google.com).
 *   2. You sign in at Google, which asks "Share your name and email with
 *      Orthodox Daily?". Your Google password never reaches us.
 *   3. Google sends your browser back to /login/oauth2/code/google with a
 *      one-time code. (That address must be listed in the Google Cloud
 *      Console as an "Authorized redirect URI": http://localhost:8080/... on
 *      your computer, https://orthodox-daily.onrender.com/... online.)
 *   4. Spring Security swaps that code with Google for an "ID token": a signed
 *      message saying who you are. It checks the signature, then
 *      SocialSignInHandler signs you in.
 *
 * A "ClientRegistration" holds a provider's addresses and our keys for it.
 * Spring Security asks this class for it by name ("google").
 */
public class SocialClientRegistrations implements ClientRegistrationRepository {

    private final ClientRegistration google; // null when not set up

    public SocialClientRegistrations(AuthProperties properties) {
        AuthProperties.Google g = properties.google();
        // Spring already knows Google's addresses; we only add our keys.
        this.google = g.configured()
                ? CommonOAuth2Provider.GOOGLE.getBuilder("google")
                        .clientId(g.clientId())
                        .clientSecret(g.clientSecret())
                        .build()
                : null;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return "google".equals(registrationId) ? google : null;
    }

    public boolean googleEnabled() {
        return google != null;
    }
}
