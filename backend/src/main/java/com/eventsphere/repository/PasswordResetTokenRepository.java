package com.eventsphere.repository;

import com.eventsphere.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** The user's codes that have not been used or invalidated yet. */
    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);

    /** The most recent still-open code (only one is open at a time). */
    Optional<PasswordResetToken> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);
}
