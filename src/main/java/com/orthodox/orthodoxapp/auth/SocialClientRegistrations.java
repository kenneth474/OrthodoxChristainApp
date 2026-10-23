package com.orthodox.orthodoxapp.auth;

import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * SocialClientRegistrations: the Google and Apple sign-in setups.
 *
 * HOW "SIGN IN WITH GOOGLE" WORKS (OAuth 2.0 / OpenID Connect)
 *   1. You click "Continue with Google". We send your browser to Google
 *      (/oauth2/authorization/google -> accounts.google.com).
 *   2. You sign in at Google, which asks "Share your name and email with
 *      Orthodox Daily?". Your Google password never reaches us.
 *   3. Google sends your browser back to /login/oauth2/code/google with a
 *      one-time code.
 *   4. Spring Security swaps that code with Google for an "ID token": a signed
 *      message saying who you are. It checks the signature, then
 *      SocialSignInHandler signs you in.
 * Apple works the same way, with a few differences noted below.
 *
 * A "ClientRegistration" holds one provider's addresses and our keys for it.
 * Spring Security asks this class for them by name ("google" or "apple").
 */
public class SocialClientRegistrations implements ClientRegistrationRepository {

    private final ClientRegistration google; // null when not set up
    private final ClientRegistration apple;  // null when not set up
    private final AppleClientSecret appleSecret;

    public SocialClientRegistrations(AuthProperties properties) {
        AuthProperties.Google g = properties.google();
        // Spring already knows Google's addresses; we only add our keys.
        this.google = g.configured()
                ? CommonOAuth2Provider.GOOGLE.getBuilder("google")
                        .clientId(g.clientId())
                        .clientSecret(g.clientSecret())
                        .build()
                : null;

        AuthProperties.Apple a = properties.apple();
        this.appleSecret = a.configured() ? AppleClientSecret.fromSettings(a) : null;
        this.apple = a.configured() ? appleRegistration(a) : null;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return switch (registrationId) {
            case "google" -> google;
            // Apple's secret expires, so hand out a copy with a fresh one.
            case "apple" -> apple == null ? null
                    : ClientRegistration.withClientRegistration(apple).clientSecret(appleSecret.current()).build();
            default -> null;
        };
    }

    public boolean googleEnabled() {
        return google != null;
    }

    public boolean appleEnabled() {
        return apple != null;
    }

    public boolean anyEnabled() {
        return googleEnabled() || appleEnabled();
    }

    // Spring doesn't know Apple's addresses, so we list them (from Apple's documentation).
    private static ClientRegistration appleRegistration(AuthProperties.Apple a) {
        return ClientRegistration.withRegistrationId("apple")
                .clientName("Apple")
                .clientId(a.clientId())
                .clientSecret("replaced-by-a-fresh-secret-on-each-use")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "name", "email")
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys") // Apple's public keys, to check ID tokens
                .issuerUri("https://appleid.apple.com")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .build();
    }
}
