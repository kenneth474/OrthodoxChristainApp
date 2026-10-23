package com.orthodox.orthodoxapp.account;

import org.springframework.data.jpa.repository.JpaRepository;

/** Loads and saves ProfilePhoto rows, by account id. */
public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, Long> {
}
