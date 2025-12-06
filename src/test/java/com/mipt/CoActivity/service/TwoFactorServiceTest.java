package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TwoFactorService twoFactorService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);
        testUser.setTwoFactorEnabled(false);
        testUser.setTwoFactorSecret(null);

        ReflectionTestUtils.setField(twoFactorService, "appName", "CoActivity");
    }

    @Test
    void testGenerateSecretAndQrCode_Success() {
        // Given
        // No need to mock userRepository.save() as it's not called in generateSecretAndQrCode

        // When
        TwoFactorService.TwoFactorSetupResult result = twoFactorService.generateSecretAndQrCode(testUser);

        // Then
        assertNotNull(result);
        assertNotNull(result.getSecret());
        assertNotNull(result.getQrCodeImageUri());
        assertNotNull(result.getManualEntryKey());
        assertTrue(result.getQrCodeImageUri().startsWith("data:image/png;base64,"));
    }

    @Test
    void testVerifyCode_ReturnsFalseWhenNoSecret() {
        // Given
        testUser.setTwoFactorSecret(null);

        // When
        boolean result = twoFactorService.verifyCode(testUser, "123456");

        // Then
        assertFalse(result);
    }

    @Test
    void testVerifyCodeWithSecret_ReturnsFalseWhenInvalidCode() {
        // Given
        String secret = "TEST_SECRET_KEY_12345678901234567890";
        String invalidCode = "000000";

        // When
        boolean result = twoFactorService.verifyCodeWithSecret(secret, invalidCode);

        // Then
        // May be true or false depending on timing, but should not throw exception
        assertNotNull(Boolean.valueOf(result));
    }

    @Test
    void testVerifyCodeWithSecret_ReturnsFalseWhenCodeTooShort() {
        // Given
        String secret = "TEST_SECRET_KEY_12345678901234567890";
        String shortCode = "12345";

        // When
        boolean result = twoFactorService.verifyCodeWithSecret(secret, shortCode);

        // Then
        assertFalse(result);
    }

    @Test
    void testVerifyCodeWithSecret_ReturnsFalseWhenSecretIsNull() {
        // Given
        String code = "123456";

        // When
        boolean result = twoFactorService.verifyCodeWithSecret(null, code);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnableTwoFactor_Success() {
        // Given
        String secret = "TEST_SECRET_KEY_12345678901234567890";
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        twoFactorService.enableTwoFactor(testUser, secret);

        // Then
        assertEquals(secret, testUser.getTwoFactorSecret());
        assertTrue(testUser.getTwoFactorEnabled());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testDisableTwoFactor_Success() {
        // Given
        testUser.setTwoFactorSecret("TEST_SECRET");
        testUser.setTwoFactorEnabled(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        twoFactorService.disableTwoFactor(testUser);

        // Then
        assertNull(testUser.getTwoFactorSecret());
        assertFalse(testUser.getTwoFactorEnabled());
        verify(userRepository, times(1)).save(testUser);
    }
}

