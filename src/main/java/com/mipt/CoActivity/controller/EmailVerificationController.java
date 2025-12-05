package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import com.mipt.CoActivity.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/email-verification")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class EmailVerificationController {
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);

    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;

    public EmailVerificationController(EmailVerificationService emailVerificationService, UserRepository userRepository) {
        this.emailVerificationService = emailVerificationService;
        this.userRepository = userRepository;
    }

    /**
     * Подтверждает email по токену
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        Map<String, String> response = new HashMap<>();

        try {
            emailVerificationService.cleanupExpiredTokens();
            emailVerificationService.verifyEmail(token);
            
            response.put("message", "Email успешно подтвержден!");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error verifying email with token: {}", token, e);
            response.put("error", e.getMessage() != null ? e.getMessage() : "Ошибка при подтверждении email");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Unexpected error verifying email", e);
            response.put("error", "Произошла ошибка при подтверждении email");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Проверяет валидность токена
     */
    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            emailVerificationService.cleanupExpiredTokens();
            boolean isValid = emailVerificationService.isTokenValid(token);
            response.put("valid", isValid);

            if (!isValid) {
                response.put("message", "Токен недействителен или истек");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error validating token", e);
            response.put("valid", false);
            response.put("message", "Ошибка при проверке токена");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Повторно отправляет письмо подтверждения email
     */
    @PostMapping("/resend")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(@RequestParam String email) {
        Map<String, String> response = new HashMap<>();

        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                // Не сообщаем, существует ли пользователь (для безопасности)
                response.put("message", "Если указанный email существует и не подтвержден, на него будет отправлено письмо для подтверждения.");
                return ResponseEntity.ok(response);
            }

            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                response.put("message", "Email уже подтвержден.");
                return ResponseEntity.ok(response);
            }

            emailVerificationService.sendVerificationEmail(user);
            response.put("message", "Письмо для подтверждения email отправлено на указанный адрес.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error resending verification email to: {}", email, e);
            response.put("error", "Ошибка при отправке письма. Пожалуйста, попробуйте еще раз.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

