package com.orthodox.orthodoxapp.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * VerificationCodeRepository: loads and saves VerificationCode rows.
 * As with AppUserRepository, Spring writes the queries from the method names:
 *   findFirstBy...OrderByCreatedAtDesc = "the newest matching code".
 */
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /** The newest code for this user and purpose that's still usable. */
    Optional<VerificationCode> findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(Long userId, CodePurpose purpose);

    /** Every still-usable code for this user and purpose. */
    List<VerificationCode> findByUserIdAndPurposeAndUsedFalse(Long userId, CodePurpose purpose);
}
