package com.orthodox.orthodoxapp.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * AuthProperties: the Google and Apple sign-in keys from secrets.properties.
 *
 *   app.auth.google.client-id=...
 *   app.auth.google.client-secret=...
 *   app.auth.apple.client-id=...          (Apple calls this the "Services ID")
 *   app.auth.apple.team-id=...
 *   app.auth.apple.key-id=...
 *   app.auth.apple.private-key-file=./AuthKey_XXXXXXXXXX.p8
 *
 * A provider's button only appears when all of its settings are filled in.
 * @DefaultValue creates an empty Google/Apple record when nothing is set,
 * instead of null, so the code can safely call google().configured().
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(@DefaultValue Google google, @DefaultValue Apple apple) {

    public record Google(String clientId, String clientSecret) {
        public boolean configured() {
            return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
        }
    }

    public record Apple(String clientId, String teamId, String keyId, String privateKeyFile) {
        public boolean configured() {
            return StringUtils.hasText(clientId) && StringUtils.hasText(teamId)
                    && StringUtils.hasText(keyId) && StringUtils.hasText(privateKeyFile);
        }
    }
}
