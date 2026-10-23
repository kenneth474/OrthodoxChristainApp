package com.orthodox.orthodoxapp.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.Length;

/**
 * ProfilePhoto: one account's photo, as a small square JPEG in the
 * "profile_photo" table.
 *
 * It's a separate table from app_user so that loading an account (which
 * happens on almost every request) doesn't drag the picture along too.
 * The id is the account's own id: one photo per account.
 *
 * length = Length.LONG32 tells the database to expect a big chunk of bytes
 * (up to 2 GB; our photos are about 20 KB). In PostgreSQL this becomes a
 * "bytea" column, which stores the bytes right in the row. We don't use @Lob
 * ("large object"): in PostgreSQL that stores the bytes in a separate place
 * that can only be read inside a transaction, and isn't cleaned up when the
 * row is deleted.
 */
@Entity
public class ProfilePhoto {

    @Id
    private Long userId;

    @Column(nullable = false, length = Length.LONG32)
    private byte[] jpeg;

    protected ProfilePhoto() {
    }

    public ProfilePhoto(Long userId, byte[] jpeg) {
        this.userId = userId;
        this.jpeg = jpeg;
    }

    public byte[] getJpeg() {
        return jpeg;
    }
}
