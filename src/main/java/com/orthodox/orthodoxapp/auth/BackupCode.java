package com.orthodox.orthodoxapp.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnDefault;

/**
 * BackupCode: one of the ten one-time codes you get when you turn on an
 * authenticator app, for signing in if your phone is lost. A row in the
 * "backup_code" table.
 *
 * As with passwords, the code itself is never stored, only a hash of it
 * (see AuthenticatorService.hash). Each code works once.
 */
@Entity
public class BackupCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String codeHash;

    @ColumnDefault("false")
    private boolean used;

    protected BackupCode() {
    }

    public BackupCode(Long userId, String codeHash) {
        this.userId = userId;
        this.codeHash = codeHash;
    }

    public void markUsed() {
        used = true;
    }
}
