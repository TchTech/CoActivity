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
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@test.com", "encodedPassword");
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmailVerified(true); // По умолчанию email подтвержден для существующих тестов
    }

    @Test
    void testLoginUser_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setLogin("test@test.com");
        request.setPassword("password");
        
        // Устанавливаем emailVerified = true для успешного входа
        testUser.setEmailVerified(true);
        
        when(userRepository.findByEmail("test@test.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password", testUser.getPasswordHash())).thenReturn(true);

        // When
        LoginResponse result = authService.loginUser(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertEquals(testUser.getId(), result.getUserId());
    }

    @Test
    void testLoginUser_ThrowsExceptionWhenUserNotFound() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setLogin("nonexistent@test.com");
        request.setPassword("password");
        
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(null);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            authService.loginUser(request);
        });
    }

    @Test
    void testLoginUser_ThrowsExceptionWhenPasswordIncorrect() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setLogin("test@test.com");
        request.setPassword("wrongpassword");
        
        testUser.setEmailVerified(true);
        
        when(userRepository.findByEmail("test@test.com")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            authService.loginUser(request);
        });
    }
    
    @Test
    void testLoginUser_ThrowsExceptionWhenEmailNotVerified() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setLogin("test@test.com");
        request.setPassword("password");
        
        // Устанавливаем emailVerified = false (не подтвержден)
        testUser.setEmailVerified(false);
        
        when(userRepository.findByEmail("test@test.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password", testUser.getPasswordHash())).thenReturn(true);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            authService.loginUser(request);
        });
    }

    @Test
    void testRegisterNewUser_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("newuser@test.com");
        request.setPassword("password123");
        
        when(userRepository.findByEmail("newuser@test.com")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        // Мокируем отправку email при регистрации
        doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

        // When
        User result = authService.registerNewUser(request);

        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailVerificationService, times(1)).sendVerificationEmail(any(User.class));
    }

    @Test
    void testRegisterNewUser_ThrowsExceptionWhenEmailExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("test@test.com");
        request.setPassword("password123");
        
        when(userRepository.findByEmail("test@test.com")).thenReturn(testUser);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            authService.registerNewUser(request);
        });
    }

    @Test
    void testRegisterNewUser_ThrowsExceptionWhenPasswordTooShort() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("newuser@test.com");
        request.setPassword("short");

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            authService.registerNewUser(request);
        });
    }
}

