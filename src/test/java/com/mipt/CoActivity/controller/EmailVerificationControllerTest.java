package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import com.mipt.CoActivity.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = EmailVerificationController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);
        testUser.setEmailVerified(false);
    }

    @Test
    void testVerifyEmail_Success() throws Exception {
        // Given
        doNothing().when(emailVerificationService).cleanupExpiredTokens();
        doNothing().when(emailVerificationService).verifyEmail("valid-token");

        // When & Then
        mockMvc.perform(post("/email-verification/verify")
                .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testVerifyEmail_ReturnsBadRequestOnInvalidToken() throws Exception {
        // Given
        doNothing().when(emailVerificationService).cleanupExpiredTokens();
        doThrow(new RuntimeException("Invalid token")).when(emailVerificationService).verifyEmail("invalid-token");

        // When & Then
        mockMvc.perform(post("/email-verification/verify")
                .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testValidateToken_ReturnsTrue() throws Exception {
        // Given
        doNothing().when(emailVerificationService).cleanupExpiredTokens();
        when(emailVerificationService.isTokenValid("valid-token")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/email-verification/validate-token")
                .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void testValidateToken_ReturnsFalse() throws Exception {
        // Given
        doNothing().when(emailVerificationService).cleanupExpiredTokens();
        when(emailVerificationService.isTokenValid("invalid-token")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/email-verification/validate-token")
                .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void testResendVerificationEmail_Success() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        doNothing().when(emailVerificationService).sendVerificationEmail(any(User.class));

        // When & Then
        mockMvc.perform(post("/email-verification/resend")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testResendVerificationEmail_UserNotFound() throws Exception {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/email-verification/resend")
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testResendVerificationEmail_AlreadyVerified() throws Exception {
        // Given
        testUser.setEmailVerified(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/email-verification/resend")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email уже подтвержден."));
    }
}

