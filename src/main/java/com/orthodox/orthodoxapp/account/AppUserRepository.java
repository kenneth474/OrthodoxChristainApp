package com.orthodox.orthodoxapp.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AppUserRepository: loads and saves AppUser rows.
 *
 * WHERE IS THE CODE?
 * There isn't any, and that's the point. This is only an interface. Spring
 * Data JPA reads the method NAMES and writes the database queries for us:
 *   findByEmail("a@b.com")  ->  SELECT * FROM app_user WHERE email = 'a@b.com'
 *
 * JpaRepository<AppUser, Long> also gives us save(), findById(), delete() and
 * more for free. "Long" is the type of AppUser's id.
 *
 * Optional<AppUser> means "maybe an AppUser, maybe nothing": the caller has to
 * decide what to do when no account matches, instead of getting a null.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<AppUser> findByGoogleId(String googleId);
}
