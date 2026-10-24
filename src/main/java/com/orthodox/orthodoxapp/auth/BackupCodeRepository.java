package com.orthodox.orthodoxapp.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** BackupCodeRepository: loads and saves BackupCode rows (Spring writes the queries from the names). */
public interface BackupCodeRepository extends JpaRepository<BackupCode, Long> {

    /** An unused backup code of this account with this hash, if there is one. */
    Optional<BackupCode> findFirstByUserIdAndCodeHashAndUsedFalse(Long userId, String codeHash);

    /** How many backup codes this account has left. */
    long countByUserIdAndUsedFalse(Long userId);

    /** Removes all of an account's backup codes (when new ones are made, or the app is turned off). */
    void deleteByUserId(Long userId);
}
