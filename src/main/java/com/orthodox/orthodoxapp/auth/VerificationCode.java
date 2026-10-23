package com.orthodox.orthodoxapp.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/**
 * VerificationCode: one 6-digit code we sent, stored as a row in the
 * "verification_code" table.
 *
 * Like passwords, the code itself is never stored, only a hash of it. Each
 * code also remembers when it expires and how many wrong guesses it has had.
 */
@Entity
public class VerificationCode {

    /** Wrong guesses allowed before a code stops working. */
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which AppUser the code belongs to (their id).
    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodePurpose purpose;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private int attempts;

    // True once the code has been used, replaced by a newer code, or guessed wrong too often.
    private boolean used;

    protected VerificationCode() {
    }

    public VerificationCode(Long userId, CodePurpose purpose, String codeHash, Instant createdAt, Instant expiresAt) {
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** The code can't be used any more. */
    public void retire() {
        used = true;
    }

    /** Counts a wrong guess; after MAX_ATTEMPTS the code stops working. */
    public void recordWrongGuess() {
        attempts++;
        if (attempts >= MAX_ATTEMPTS) {
            used = true;
        }
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public String getCodeHash() { return codeHash; }

    public Instant getCreatedAt() { return createdAt; }

    public boolean isUsed() { return used; }
}
