package com.orthodox.orthodoxapp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * We can't call Apple from a test, so we make our own key pair (the same
 * kind Apple issues), sign a secret with the private half, and check it with
 * the public half, just as Apple would.
 */
class AppleClientSecretTest {

    @Test
    void createsASignedSecretWithTheFieldsAppleExpects() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1")); // the curve Apple uses (P-256)
        KeyPair keys = generator.generateKeyPair();

        // Write the private key as a .p8 file's text would look.
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keys.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        AppleClientSecret secret = new AppleClientSecret("TEAM123456", "KEY1234567", "org.example.app", pem);
        Instant now = Instant.parse("2026-10-23T12:00:00Z");
        SignedJWT jwt = SignedJWT.parse(secret.create(now));

        assertTrue(jwt.verify(new ECDSAVerifier((ECPublicKey) keys.getPublic())), "signature should check out");
        assertEquals("KEY1234567", jwt.getHeader().getKeyID());
        assertEquals("TEAM123456", jwt.getJWTClaimsSet().getIssuer());
        assertEquals("org.example.app", jwt.getJWTClaimsSet().getSubject());
        assertEquals(List.of("https://appleid.apple.com"), jwt.getJWTClaimsSet().getAudience());
        assertEquals(now.plusSeconds(3600), jwt.getJWTClaimsSet().getExpirationTime().toInstant());
    }

    @Test
    void aBrokenKeyFileGivesAClearError() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new AppleClientSecret("T", "K", "C", "not a key"));
        assertEquals("The Apple key file isn't a valid .p8 private key", ex.getMessage());
    }
}
