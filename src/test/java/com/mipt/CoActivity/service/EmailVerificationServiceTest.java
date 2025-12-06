package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.EmailVerificationToken;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.EmailVerificationTokenRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;
    private EmailVerificationToken testToken;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);
        testUser.setEmailVerified(false);

        testToken = new EmailVerificationToken(testUser, Instant.now().plusSeconds(86400));
        testToken.setToken("test-token-123");
        testToken.setUsed(false);

        ReflectionTestUtils.setField(emailVerificationService, "tokenExpirationHours", 24);
    }

    @Test
    void testSendVerificationEmail_Success() {
        // Given
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);
        doNothing().when(emailService).sendEmailVerificationEmail(anyString(), anyString());

        // When
        emailVerificationService.sendVerificationEmail(testUser);

        // Then
        verify(tokenRepository, times(1)).deleteByUser(testUser);
        verify(tokenRepository, times(1)).save(any(EmailVerificationToken.class));
        verify(emailService, times(1)).sendEmailVerificationEmail(testUser.getEmail(), anyString());
    }

    @Test
    void testSendVerificationEmail_DeletesTokenOnEmailFailure() {
        // Given
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);
        doThrow(new RuntimeException("Email send failed")).when(emailService).sendEmailVerificationEmail(anyString(), anyString());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            emailVerificationService.sendVerificationEmail(testUser);
        });
        verify(tokenRepository, times(1)).delete(any(EmailVerificationToken.class));
    }

    @Test
    void testVerifyEmail_Success() {
        // Given
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        emailVerificationService.verifyEmail("test-token-123");

        // Then
        assertTrue(testUser.getEmailVerified());
        assertTrue(testToken.getUsed());
        verify(userRepository, times(1)).save(testUser);
        verify(tokenRepository, times(1)).deleteByUser(testUser);
    }

    @Test
    void testVerifyEmail_ThrowsExceptionWhenTokenNotFound() {
        // Given
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            emailVerificationService.verifyEmail("invalid-token");
        });
    }

    @Test
    void testVerifyEmail_ThrowsExceptionWhenTokenExpired() {
        // Given
        testToken.setExpiryDate(Instant.now().minusSeconds(3600)); // Expired 1 hour ago
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            emailVerificationService.verifyEmail("test-token-123");
        });
        verify(tokenRepository, times(1)).delete(testToken);
    }

    @Test
    void testVerifyEmail_ThrowsExceptionWhenTokenAlreadyUsed() {
        // Given
        testToken.setUsed(true);
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            emailVerificationService.verifyEmail("test-token-123");
        });
    }

    @Test
    void testIsTokenValid_ReturnsTrue() {
        // Given
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));

        // When
        boolean result = emailVerificationService.isTokenValid("test-token-123");

        // Then
        assertTrue(result);
    }

    @Test
    void testIsTokenValid_ReturnsFalseWhenTokenNotFound() {
        // Given
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When
        boolean result = emailVerificationService.isTokenValid("invalid-token");

        // Then
        assertFalse(result);
    }

    @Test
    void testCleanupExpiredTokens_Success() {
        // Given
        doNothing().when(tokenRepository).deleteByExpiryDateBefore(any(Instant.class));

        // When
        emailVerificationService.cleanupExpiredTokens();

        // Then
        verify(tokenRepository, times(1)).deleteByExpiryDateBefore(any(Instant.class));
    }
}

