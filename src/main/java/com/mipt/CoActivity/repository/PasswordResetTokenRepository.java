package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.PasswordResetToken;
import com.mipt.CoActivity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);
    void deleteByUser(User user);
    void deleteByExpiryDateBefore(java.time.Instant now);
}

