package com.orthodox.orthodoxapp.auth;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Totp: the 6-digit codes of authenticator apps (Google Authenticator,
 * Microsoft Authenticator, the iPhone's Passwords app, 1Password...).
 *
 * HOW IT WORKS ("TOTP", time-based one-time passwords, standard RFC 6238)
 * When you set it up, the app and our server share a random secret (in the
 * QR code). Every 30 seconds, both work out the same 6-digit code from that
 * secret and the current time, without talking to each other:
 *     code = the last 6 digits of HMAC-SHA1(secret, number of 30-second steps since 1970)
 * So the app never needs the internet, and a code is useless after a minute.
 *
 * Secrets are written in Base32 (letters A to Z and digits 2 to 7), the
 * format authenticator apps expect.
 */
public final class Totp {

    static final int DIGITS = 6;
    static final long STEP_SECONDS = 30;

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Totp() {
        // only static methods; never created
    }

    /** A new random secret: 20 bytes (160 bits, as the standard recommends), in Base32. */
    public static String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** Which 30-second step a moment falls in. */
    public static long stepAt(Instant time) {
        return Math.floorDiv(time.getEpochSecond(), STEP_SECONDS);
    }

    /** The 6-digit code for a secret at a given step, e.g. "287082". */
    public static String code(String secretBase32, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(secretBase32), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());

            // "Dynamic truncation": the last 4 bits choose where in the hash to
            // read 4 bytes; those make a number whose last 6 digits are the code.
            int offset = hash[hash.length - 1] & 0x0f;
            int number = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return "%06d".formatted(number % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is missing from this Java", e);
        }
    }

    /**
     * Checks a typed code. Accepts the current step and one step either side,
     * because a phone's clock can be a few seconds off and typing takes time.
     *
     * @return the step the code matched, or empty if it's wrong
     */
    public static OptionalLong verify(String secretBase32, String typed, Instant now) {
        String digits = typed == null ? "" : typed.replaceAll("\\s", "");
        if (!digits.matches("\\d{" + DIGITS + "}")) {
            return OptionalLong.empty();
        }
        long current = stepAt(now);
        for (long step = current - 1; step <= current + 1; step++) {
            // MessageDigest.isEqual compares in constant time: how long the
            // check takes reveals nothing about how many digits were right.
            if (MessageDigest.isEqual(
                    code(secretBase32, step).getBytes(StandardCharsets.US_ASCII),
                    digits.getBytes(StandardCharsets.US_ASCII))) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }

    /**
     * The "otpauth://" address inside the QR code. Authenticator apps read it
     * to add the account; on an iPhone, tapping it opens the Passwords app.
     */
    public static String setupUri(String secretBase32, String accountEmail) {
        String issuer = "Orthodox Daily";
        String label = encode(issuer + ":" + accountEmail);
        return "otpauth://totp/" + label + "?secret=" + secretBase32 + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
    }

    // ----- Base32 (RFC 4648), without "=" padding, as authenticator apps use it -----

    static String base32Encode(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }
        return out.toString();
    }

    static byte[] base32Decode(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : text.toUpperCase(Locale.ROOT).replaceAll("[\\s=]", "").toCharArray()) {
            int value = BASE32.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Not a Base32 letter: " + c);
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
