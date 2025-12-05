package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.model.PasswordResetToken;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.PasswordResetTokenRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${app.password-reset.token-expiration-hours:24}")
    private int tokenExpirationHours;
    
    @Autowired
    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email);
        
        // Для безопасности не сообщаем, существует ли пользователь
        if (user == null) {
            logger.warn("Password reset requested for non-existent email: {}", email);
            return; // Возвращаемся без ошибки для безопасности
        }
        
        // Удаляем старые токены для этого пользователя
        tokenRepository.deleteByUser(user);
        
        // Создаем новый токен
        Instant expiryDate = Instant.now().plus(tokenExpirationHours, ChronoUnit.HOURS);
        PasswordResetToken token = new PasswordResetToken(user, expiryDate);
        tokenRepository.save(token);
        
        // Отправляем email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
            logger.info("Password reset token created and email sent for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send password reset email", e);
            // Удаляем токен, если не удалось отправить email
            tokenRepository.delete(token);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        
        // Проверяем, что токен валиден
        if (!token.isValid()) {
            if (token.isExpired()) {
                tokenRepository.delete(token);
                throw new BadRequestException("Reset token has expired. Please request a new one.");
            }
            if (token.getUsed()) {
                throw new BadRequestException("Reset token has already been used.");
            }
        }
        
        // Обновляем пароль
        User user = token.getUser();
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
        
        // Помечаем токен как использованный
        token.setUsed(true);
        tokenRepository.save(token);
        
        // Удаляем все другие токены для этого пользователя
        tokenRepository.deleteByUser(user);
        
        logger.info("Password reset successfully for user: {}", user.getEmail());
    }
    
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        tokenRepository.deleteByExpiryDateBefore(now);
        logger.info("Cleaned up expired password reset tokens");
    }
    
    public boolean isTokenValid(String tokenValue) {
        try {
            PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                    .orElse(null);
            
            if (token == null) {
                return false;
            }
            
            return token.isValid();
        } catch (Exception e) {
            logger.error("Error validating token", e);
            return false;
        }
    }
}

