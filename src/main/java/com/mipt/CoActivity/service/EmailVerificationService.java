package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.EmailVerificationToken;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.EmailVerificationTokenRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EmailVerificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.email-verification.token-expiration-hours:24}")
    private int tokenExpirationHours;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Генерирует токен подтверждения email и отправляет письмо
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        // Удаляем все существующие токены для этого пользователя
        tokenRepository.deleteByUser(user);

        // Создаем новый токен
        Instant expiryDate = Instant.now().plus(tokenExpirationHours, ChronoUnit.HOURS);
        EmailVerificationToken token = new EmailVerificationToken(user, expiryDate);
        tokenRepository.save(token);

        // Отправляем письмо
        logger.info("Preparing to send verification email to user: {}", user.getEmail());
        logger.info("Token generated: {}", token.getToken().substring(0, Math.min(10, token.getToken().length())) + "...");
        
        try {
            logger.info("Calling emailService.sendEmailVerificationEmail()...");
            emailService.sendEmailVerificationEmail(user.getEmail(), token.getToken());
            logger.info("✓✓✓ Email verification email sent successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("✗✗✗ CRITICAL ERROR: Failed to send email verification email for user: {}", user.getEmail());
            logger.error("Exception type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Root cause: {}", e.getCause().getMessage());
                logger.error("Root cause type: {}", e.getCause().getClass().getName());
            }
            logger.error("Full exception stack trace:", e);
            
            // Удаляем токен, если не удалось отправить email
            logger.warn("Deleting token due to email send failure");
            tokenRepository.delete(token);
            
            throw new RuntimeException("Failed to send email verification email: " + e.getMessage(), e);
        }
    }

    /**
     * Подтверждает email пользователя по токену
     */
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            throw new RuntimeException("Verification token has expired");
        }

        if (Boolean.TRUE.equals(verificationToken.getUsed())) {
            throw new RuntimeException("Verification token has already been used");
        }

        // Помечаем токен как использованный
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        // Помечаем email как подтвержденный
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        logger.info("Email verified for user: {}", user.getEmail());

        // Удаляем все другие токены для этого пользователя
        tokenRepository.deleteByUser(user);
    }

    /**
     * Проверяет, действителен ли токен
     */
    public boolean isTokenValid(String token) {
        return tokenRepository.findByToken(token)
                .map(EmailVerificationToken::isValid)
                .orElse(false);
    }

    /**
     * Очищает истекшие токены
     */
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        tokenRepository.deleteByExpiryDateBefore(now);
        logger.info("Cleaned up expired email verification tokens");
    }
}

