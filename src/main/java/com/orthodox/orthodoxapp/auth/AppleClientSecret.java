package com.orthodox.orthodoxapp.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * AppleClientSecret: makes the "client secret" Sign in with Apple needs.
 *
 * Google gives you a fixed secret (a long password). Apple doesn't: instead
 * you download a private key file (AuthKey_XXXX.p8) and use it to SIGN a
 * short message saying "I am this app". That signed message is a JWT (JSON
 * Web Token), and it's only valid for a limited time, so we make a fresh one
 * every hour.
 *
 * A digital signature proves the message came from whoever holds the private
 * key, without revealing the key. Apple checks it with the matching public key.
 */
public class AppleClientSecret {

    private static final Duration VALID_FOR = Duration.ofHours(1);

    private final String teamId;
    private final String keyId;
    private final String clientId;
    private final ECPrivateKey privateKey;

    private String current;
    private Instant renewAt = Instant.EPOCH;

    public AppleClientSecret(String teamId, String keyId, String clientId, String privateKeyPem) {
        this.teamId = teamId;
        this.keyId = keyId;
        this.clientId = clientId;
        this.privateKey = parsePrivateKey(privateKeyPem);
    }

    /** Reads the .p8 key file named in secrets.properties. */
    public static AppleClientSecret fromSettings(AuthProperties.Apple apple) {
        try {
            String pem = Files.readString(Path.of(apple.privateKeyFile()));
            return new AppleClientSecret(apple.teamId(), apple.keyId(), apple.clientId(), pem);
        } catch (IOException ex) {
            throw new IllegalStateException("Couldn't read the Apple key file " + apple.privateKeyFile(), ex);
        }
    }

    /**
     * A valid secret, renewed a few minutes before the old one expires.
     * "synchronized" = only one request at a time runs this method, so two
     * requests can't both renew the secret at once.
     */
    public synchronized String current() {
        Instant now = Instant.now();
        if (current == null || !now.isBefore(renewAt)) {
            current = create(now);
            renewAt = now.plus(VALID_FOR).minus(Duration.ofMinutes(5));
        }
        return current;
    }

    /** Builds and signs the JWT, with the fields Apple's documentation asks for. */
    String create(Instant now) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(teamId)                          // who made it: our Apple team
                .subject(clientId)                       // which app it's for
                .audience("https://appleid.apple.com")   // who it's meant for: Apple
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(VALID_FOR)))
                .build();
        // ES256 = the signature method Apple keys use; kid tells Apple which key signed it.
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(), claims);
        try {
            jwt.sign(new ECDSASigner(privateKey));
        } catch (JOSEException ex) {
            throw new IllegalStateException("Couldn't sign the Apple client secret", ex);
        }
        return jwt.serialize(); // the text form: header.claims.signature
    }

    /**
     * Turns the text of a .p8 file into a key Java can use. The file is
     * "PEM" format: base64 text between -----BEGIN/END PRIVATE KEY----- lines.
     */
    static ECPrivateKey parsePrivateKey(String pem) {
        String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (IllegalArgumentException | GeneralSecurityException | ClassCastException ex) {
            throw new IllegalStateException("The Apple key file isn't a valid .p8 private key", ex);
        }
    }
}
