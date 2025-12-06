package com.mipt.CoActivity.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Set default values using reflection
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@coactivity.com");
        ReflectionTestUtils.setField(emailService, "passwordResetBaseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(emailService, "emailVerificationBaseUrl", "http://localhost:3000");
    }

    @Test
    void testSendPasswordResetEmail_Success() throws Exception {
        // Given
        String email = "test@example.com";
        String token = "test-token-123";

        // When
        emailService.sendPasswordResetEmail(email, token);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail_ThrowsExceptionOnError() throws Exception {
        // Given
        String email = "test@example.com";
        String token = "test-token-123";
        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            emailService.sendPasswordResetEmail(email, token);
        });
    }

    @Test
    void testSendSimpleEmail_Success() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String text = "Test message";

        // When
        emailService.sendSimpleEmail(to, subject, text);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendSimpleEmail_ThrowsExceptionOnError() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String text = "Test message";
        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            emailService.sendSimpleEmail(to, subject, text);
        });
    }

    @Test
    void testSendEmailVerificationEmail_Success() throws Exception {
        // Given
        String email = "test@example.com";
        String token = "verification-token-123";

        // When
        emailService.sendEmailVerificationEmail(email, token);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailVerificationEmail_ThrowsExceptionOnError() throws Exception {
        // Given
        String email = "test@example.com";
        String token = "verification-token-123";
        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            emailService.sendEmailVerificationEmail(email, token);
        });
    }

    @Test
    void testSendEmailVerificationResendEmail_Success() throws Exception {
        // Given
        String email = "test@example.com";
        String token = "verification-token-123";

        // When
        emailService.sendEmailVerificationResendEmail(email, token);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailVerificationResendEmail_ThrowsExceptionOnError() throws Exception {
        // Given
        String email = "test@example.com";
        String token = "verification-token-123";
        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            emailService.sendEmailVerificationResendEmail(email, token);
        });
    }
}

