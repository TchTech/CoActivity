package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.TwoFactorVerifyRequest;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import com.mipt.CoActivity.service.TwoFactorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TwoFactorController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class TwoFactorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TwoFactorService twoFactorService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private TwoFactorService.TwoFactorSetupResult setupResult;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);
        testUser.setTwoFactorEnabled(false);
        testUser.setTwoFactorSecret(null);

        setupResult = new TwoFactorService.TwoFactorSetupResult(
                "TEST_SECRET",
                "data:image/png;base64,test",
                "TEST SECRET"
        );
    }

    @Test
    void testEnableTwoFactor_Success() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(twoFactorService.generateSecretAndQrCode(testUser)).thenReturn(setupResult);

        // When & Then
        mockMvc.perform(post("/auth/2fa/enable/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").exists())
                .andExpect(jsonPath("$.qrCodeUrl").exists());
    }

    @Test
    void testEnableTwoFactor_ThrowsExceptionWhenUserNotFound() throws Exception {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/auth/2fa/enable/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testEnableTwoFactor_ThrowsExceptionWhenAlreadyEnabled() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/auth/2fa/enable/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testVerifySetup_Success() throws Exception {
        // Given
        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest();
        request.setCode("123456");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(twoFactorService.verifyCodeWithSecret("TEST_SECRET", "123456")).thenReturn(true);
        doNothing().when(twoFactorService).enableTwoFactor(any(User.class), anyString());

        // When & Then
        mockMvc.perform(post("/auth/2fa/verify-setup/1")
                .param("secret", "TEST_SECRET")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testVerifySetup_ReturnsBadRequestOnInvalidCode() throws Exception {
        // Given
        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest();
        request.setCode("000000");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(twoFactorService.verifyCodeWithSecret("TEST_SECRET", "000000")).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/auth/2fa/verify-setup/1")
                .param("secret", "TEST_SECRET")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testDisableTwoFactor_Success() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(twoFactorService).disableTwoFactor(testUser);

        // When & Then
        mockMvc.perform(post("/auth/2fa/disable/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testDisableTwoFactor_ThrowsExceptionWhenNotEnabled() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/auth/2fa/disable/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetTwoFactorStatus_ReturnsEnabled() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/auth/2fa/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void testGetTwoFactorStatus_ReturnsDisabled() throws Exception {
        // Given
        testUser.setTwoFactorEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/auth/2fa/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}

