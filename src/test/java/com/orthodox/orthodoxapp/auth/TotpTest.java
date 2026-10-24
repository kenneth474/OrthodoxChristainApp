package com.orthodox.orthodoxapp.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Checks Totp against the official examples in the TOTP standard
 * (RFC 6238, appendix B), so our codes match every authenticator app.
 */
class TotpTest {

    // The standard's test secret: the ASCII text "12345678901234567890".
    private static final String SECRET =
            Totp.base32Encode("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    void matchesTheStandardsExamples() {
        // The standard lists 8-digit codes; ours are their last 6 digits.
        assertThat(Totp.code(SECRET, Totp.stepAt(Instant.ofEpochSecond(59)))).isEqualTo("287082");
        assertThat(Totp.code(SECRET, Totp.stepAt(Instant.ofEpochSecond(1111111109)))).isEqualTo("081804");
        assertThat(Totp.code(SECRET, Totp.stepAt(Instant.ofEpochSecond(1111111111)))).isEqualTo("050471");
        assertThat(Totp.code(SECRET, Totp.stepAt(Instant.ofEpochSecond(1234567890)))).isEqualTo("005924");
        assertThat(Totp.code(SECRET, Totp.stepAt(Instant.ofEpochSecond(2000000000)))).isEqualTo("279037");
    }

    @Test
    void base32GoesBothWays() {
        assertThat(SECRET).isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        assertThat(Totp.base32Decode("gezd gnbv gy3t qojq gezd gnbv gy3t qojq"))
                .isEqualTo("12345678901234567890".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void acceptsTheStepBeforeAndAfterButNotFurther() {
        Instant now = Instant.ofEpochSecond(1234567890);
        long step = Totp.stepAt(now);

        assertThat(Totp.verify(SECRET, "005924", now)).hasValue(step);
        assertThat(Totp.verify(SECRET, Totp.code(SECRET, step - 1), now)).hasValue(step - 1);
        assertThat(Totp.verify(SECRET, Totp.code(SECRET, step + 1), now)).hasValue(step + 1);
        assertThat(Totp.verify(SECRET, Totp.code(SECRET, step - 2), now)).isEmpty();
        assertThat(Totp.verify(SECRET, "005 924", now)).hasValue(step); // spaces are fine
        assertThat(Totp.verify(SECRET, "12345", now)).isEmpty();
        assertThat(Totp.verify(SECRET, null, now)).isEmpty();
    }

    @Test
    void newSecretsAreRandomAndTheRightLength() {
        String one = Totp.newSecret();
        assertThat(one).hasSize(32).matches("[A-Z2-7]+"); // 20 bytes = 32 Base32 letters
        assertThat(Totp.newSecret()).isNotEqualTo(one);
    }

    @Test
    void theSetupAddressNamesTheAppAndAccount() {
        assertThat(Totp.setupUri("ABC", "anna@example.com")).isEqualTo(
                "otpauth://totp/Orthodox%20Daily%3Aanna%40example.com?secret=ABC&issuer=Orthodox%20Daily"
                        + "&algorithm=SHA1&digits=6&period=30");
    }
}
