package com.orthodox.orthodoxapp.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * ProfilePhoto: one account's photo, as a small square JPEG in the
 * "profile_photo" table.
 *
 * It's a separate table from app_user so that loading an account (which
 * happens on almost every request) doesn't drag the picture along too.
 * The id is the account's own id: one photo per account.
 *
 * @Lob ("large object") tells the database to expect a big chunk of bytes.
 */
@Entity
public class ProfilePhoto {

    @Id
    private Long userId;

    @Lob
    @Column(nullable = false)
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
