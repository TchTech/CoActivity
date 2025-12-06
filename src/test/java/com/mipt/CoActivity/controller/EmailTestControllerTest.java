package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTestControllerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailTestController emailTestController;

    @BeforeEach
    void setUp() {
        // Default: no exceptions
    }

    @Test
    void testTestEmail_Success() {
        // Given
        String testEmail = "test@example.com";
        doNothing().when(emailService).sendSimpleEmail(anyString(), anyString(), anyString());

        // When
        ResponseEntity<Map<String, String>> response = emailTestController.testEmail(testEmail);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));
        assertTrue(response.getBody().get("message").contains(testEmail));
        verify(emailService, times(1)).sendSimpleEmail(eq(testEmail), anyString(), anyString());
    }

    @Test
    void testTestEmail_WithException() {
        // Given
        String testEmail = "test@example.com";
        doThrow(new RuntimeException("Email service error")).when(emailService)
                .sendSimpleEmail(anyString(), anyString(), anyString());

        // When
        ResponseEntity<Map<String, String>> response = emailTestController.testEmail(testEmail);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("error", response.getBody().get("status"));
        assertTrue(response.getBody().get("message").contains("Ошибка отправки"));
        verify(emailService, times(1)).sendSimpleEmail(eq(testEmail), anyString(), anyString());
    }
}

