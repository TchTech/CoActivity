package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.EmailVerificationToken;
import com.mipt.CoActivity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    Optional<EmailVerificationToken> findByUser(User user);
    void deleteByUser(User user);
    void deleteByExpiryDateBefore(java.time.Instant now);
}

