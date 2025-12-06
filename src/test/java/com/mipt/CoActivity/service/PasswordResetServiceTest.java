package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.model.PasswordResetToken;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.PasswordResetTokenRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken testToken;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "oldPassword");
        testUser.setId(1L);

        testToken = new PasswordResetToken(testUser, Instant.now().plusSeconds(86400));
        testToken.setToken("test-token-123");
        testToken.setUsed(false);

        ReflectionTestUtils.setField(passwordResetService, "tokenExpirationHours", 24);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    }

    @Test
    void testRequestPasswordReset_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // When
        passwordResetService.requestPasswordReset("test@example.com");

        // Then
        verify(tokenRepository, times(1)).deleteByUser(testUser);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail("test@example.com", anyString());
    }

    @Test
    void testRequestPasswordReset_UserNotFound_ReturnsSilently() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        // When
        passwordResetService.requestPasswordReset("nonexistent@example.com");

        // Then
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void testRequestPasswordReset_DeletesTokenOnEmailFailure() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);
        doThrow(new RuntimeException("Email send failed")).when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            passwordResetService.requestPasswordReset("test@example.com");
        });
        verify(tokenRepository, times(1)).delete(any(PasswordResetToken.class));
    }

    @Test
    void testResetPassword_Success() {
        // Given
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        passwordResetService.resetPassword("test-token-123", "newPassword123");

        // Then
        assertTrue(testToken.getUsed());
        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userRepository, times(1)).save(testUser);
        verify(tokenRepository, times(1)).deleteByUser(testUser);
    }

    @Test
    void testResetPassword_ThrowsExceptionWhenTokenNotFound() {
        // Given
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            passwordResetService.resetPassword("invalid-token", "newPassword123");
        });
    }

    @Test
    void testResetPassword_ThrowsExceptionWhenTokenExpired() {
        // Given
        testToken.setExpiryDate(Instant.now().minusSeconds(3600)); // Expired 1 hour ago
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            passwordResetService.resetPassword("test-token-123", "newPassword123");
        });
        verify(tokenRepository, times(1)).delete(testToken);
    }

    @Test
    void testResetPassword_ThrowsExceptionWhenTokenAlreadyUsed() {
        // Given
        testToken.setUsed(true);
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            passwordResetService.resetPassword("test-token-123", "newPassword123");
        });
    }

    @Test
    void testIsTokenValid_ReturnsTrue() {
        // Given
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));

        // When
        boolean result = passwordResetService.isTokenValid("test-token-123");

        // Then
        assertTrue(result);
    }

    @Test
    void testIsTokenValid_ReturnsFalseWhenTokenNotFound() {
        // Given
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When
        boolean result = passwordResetService.isTokenValid("invalid-token");

        // Then
        assertFalse(result);
    }

    @Test
    void testIsTokenValid_ReturnsFalseOnException() {
        // Given
        when(tokenRepository.findByToken("test-token-123")).thenThrow(new RuntimeException("Database error"));

        // When
        boolean result = passwordResetService.isTokenValid("test-token-123");

        // Then
        assertFalse(result);
    }

    @Test
    void testCleanupExpiredTokens_Success() {
        // Given
        doNothing().when(tokenRepository).deleteByExpiryDateBefore(any(Instant.class));

        // When
        passwordResetService.cleanupExpiredTokens();

        // Then
        verify(tokenRepository, times(1)).deleteByExpiryDateBefore(any(Instant.class));
    }
}

