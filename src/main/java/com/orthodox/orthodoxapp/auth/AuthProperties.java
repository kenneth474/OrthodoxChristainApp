package com.orthodox.orthodoxapp.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * AuthProperties: the Google sign-in keys, from secrets.properties on your
 * computer, or from environment variables online:
 *
 *   app.auth.google.client-id=...       (online: APP_AUTH_GOOGLE_CLIENTID)
 *   app.auth.google.client-secret=...   (online: APP_AUTH_GOOGLE_CLIENTSECRET)
 *
 * The "Continue with Google" button only works when both are filled in.
 * @DefaultValue creates an empty Google record when nothing is set, instead
 * of null, so the code can safely call google().configured().
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(@DefaultValue Google google) {

    public record Google(String clientId, String clientSecret) {
        public boolean configured() {
            return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
        }
    }
}
