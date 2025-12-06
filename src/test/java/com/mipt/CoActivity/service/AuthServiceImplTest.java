package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.LoginRequest;
import com.mipt.CoActivity.dto.LoginResponse;
import com.mipt.CoActivity.dto.RegisterRequest;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.UnauthorizedException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "encodedPassword");
        testUser.setId(1L);
        testUser.setEmailVerified(true);
        testUser.setTwoFactorEnabled(false);

        registerRequest = new RegisterRequest();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setLogin("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void testRegisterNewUser_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

        // When
        User result = authService.registerNewUser(registerRequest);

        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailVerificationService, times(1)).sendVerificationEmail(any(User.class));
    }

    @Test
    void testRegisterNewUser_ThrowsExceptionWhenEmailExists() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            authService.registerNewUser(registerRequest);
        });
    }

    @Test
    void testRegisterNewUser_ThrowsExceptionWhenNameEmpty() {
        // Given
        registerRequest.setName("");

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            authService.registerNewUser(registerRequest);
        });
    }

    @Test
    void testRegisterNewUser_ThrowsExceptionWhenPasswordTooShort() {
        // Given
        registerRequest.setPassword("short");

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            authService.registerNewUser(registerRequest);
        });
    }

    @Test
    void testLoginUser_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // When
        LoginResponse result = authService.loginUser(loginRequest);

        // Then
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertFalse(result.getRequiresTwoFactor());
    }

    @Test
    void testLoginUser_ThrowsExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(null);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            authService.loginUser(loginRequest);
        });
    }

    @Test
    void testLoginUser_ThrowsExceptionWhenPasswordInvalid() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            loginRequest.setPassword("wrongpassword");
            authService.loginUser(loginRequest);
        });
    }

    @Test
    void testLoginUser_ThrowsExceptionWhenEmailNotVerified() {
        // Given
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            authService.loginUser(loginRequest);
        });
    }

    @Test
    void testLoginUser_ReturnsRequiresTwoFactorWhenEnabled() {
        // Given
        testUser.setTwoFactorEnabled(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // When
        LoginResponse result = authService.loginUser(loginRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.getRequiresTwoFactor());
        assertNull(result.getToken());
    }

    @Test
    void testLoginUser_WithUsername() {
        // Given
        loginRequest.setLogin("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // When
        LoginResponse result = authService.loginUser(loginRequest);

        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(anyString());
    }
}

