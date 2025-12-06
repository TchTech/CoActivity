package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.PasswordResetConfirmRequest;
import com.mipt.CoActivity.dto.PasswordResetRequest;
import com.mipt.CoActivity.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PasswordResetController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordResetService passwordResetService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRequestPasswordReset_Success() throws Exception {
        // Given
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("test@example.com");
        doNothing().when(passwordResetService).requestPasswordReset("test@example.com");

        // When & Then
        mockMvc.perform(post("/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testConfirmPasswordReset_Success() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("valid-token");
        request.setNewPassword("newPassword123");
        doNothing().when(passwordResetService).resetPassword("valid-token", "newPassword123");

        // When & Then
        mockMvc.perform(post("/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testConfirmPasswordReset_ReturnsBadRequestOnError() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("invalid-token");
        request.setNewPassword("newPassword123");
        doThrow(new RuntimeException("Invalid token")).when(passwordResetService).resetPassword("invalid-token", "newPassword123");

        // When & Then
        mockMvc.perform(post("/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testValidateToken_ReturnsTrue() throws Exception {
        // Given
        doNothing().when(passwordResetService).cleanupExpiredTokens();
        when(passwordResetService.isTokenValid("valid-token")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/password-reset/validate-token")
                .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void testValidateToken_ReturnsFalse() throws Exception {
        // Given
        doNothing().when(passwordResetService).cleanupExpiredTokens();
        when(passwordResetService.isTokenValid("invalid-token")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/password-reset/validate-token")
                .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}

