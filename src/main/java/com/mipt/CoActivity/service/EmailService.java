package com.mipt.CoActivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from:noreply@coactivity.com}")
    private String fromEmail;
    
    @Value("${app.password-reset.base-url:http://localhost:3000}")
    private String passwordResetBaseUrl;
    
    @Value("${app.email-verification.base-url:http://localhost:3000}")
    private String emailVerificationBaseUrl;
    
    @Value("${spring.mail.host:smtp.yandex.ru}")
    private String mailHost;
    
    @Value("${spring.mail.port:587}")
    private Integer mailPort;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Сброс пароля - CoActivity");
            
            String resetLink = passwordResetBaseUrl + "/reset-password?token=" + token;
            String emailBody = String.format(
                "Здравствуйте!\n\n" +
                "Вы запросили сброс пароля для вашей учетной записи CoActivity.\n\n" +
                "Для сброса пароля перейдите по следующей ссылке:\n" +
                "%s\n\n" +
                "Эта ссылка действительна в течение 24 часов.\n\n" +
                "Если вы не запрашивали сброс пароля, просто проигнорируйте это письмо.\n\n" +
                "С уважением,\n" +
                "Команда CoActivity",
                resetLink
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            
            logger.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            logger.info("Email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    public void sendEmailVerificationEmail(String toEmail, String token) {
        logger.info("=== Starting email verification email send ===");
        logger.info("To: {}, From: {}, Host: {}, Port: {}, BaseUrl: {}", toEmail, fromEmail, mailHost, mailPort, emailVerificationBaseUrl);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Подтверждение email - CoActivity");
            
            String verificationLink = emailVerificationBaseUrl + "/verify-email?token=" + token;
            logger.info("Verification link generated: {}", verificationLink);
            
            String emailBody = String.format(
                "Здравствуйте!\n\n" +
                "Благодарим за регистрацию в CoActivity!\n\n" +
                "Для завершения регистрации и активации вашего аккаунта, пожалуйста, подтвердите ваш email, перейдя по следующей ссылке:\n" +
                "%s\n\n" +
                "Эта ссылка действительна в течение 24 часов.\n\n" +
                "Если вы не регистрировались в CoActivity, просто проигнорируйте это письмо.\n\n" +
                "С уважением,\n" +
                "Команда CoActivity",
                verificationLink
            );
            
            message.setText(emailBody);
            
            logger.info("Attempting to send email via mailSender...");
            mailSender.send(message);
            
            logger.info("✓ Email verification email successfully sent to: {}", toEmail);
            logger.info("=== Email send completed successfully ===");
        } catch (Exception e) {
            logger.error("✗✗✗ FAILED to send email verification email to: {}", toEmail);
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            logger.error("Email configuration - from: {}, baseUrl: {}, host: {}, port: {}", 
                        fromEmail, emailVerificationBaseUrl, mailHost, mailPort);
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            logger.error("Full stack trace:", e);
            throw new RuntimeException("Failed to send email verification: " + e.getMessage(), e);
        }
    }
    
    public void sendEmailVerificationResendEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Подтверждение email - CoActivity");
            
            String verificationLink = emailVerificationBaseUrl + "/verify-email?token=" + token;
            String emailBody = String.format(
                "Здравствуйте!\n\n" +
                "Вы запросили повторную отправку письма для подтверждения email.\n\n" +
                "Для подтверждения вашего email перейдите по следующей ссылке:\n" +
                "%s\n\n" +
                "Эта ссылка действительна в течение 24 часов.\n\n" +
                "Если вы не запрашивали это письмо, просто проигнорируйте его.\n\n" +
                "С уважением,\n" +
                "Команда CoActivity",
                verificationLink
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            
            logger.info("Email verification resend email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send email verification resend email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

