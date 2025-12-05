package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.dto.PasswordResetConfirmRequest;
import com.mipt.CoActivity.dto.PasswordResetRequest;
import com.mipt.CoActivity.service.PasswordResetService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/password-reset")
public class PasswordResetController {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);
    
    private final PasswordResetService passwordResetService;
    
    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }
    
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.requestPasswordReset(request.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Если указанный email зарегистрирован, на него будет отправлено письмо с инструкциями по сбросу пароля.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error requesting password reset", e);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Если указанный email зарегистрирован, на него будет отправлено письмо с инструкциями по сбросу пароля.");
            // Возвращаем тот же ответ для безопасности
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Пароль успешно изменен. Теперь вы можете войти с новым паролем.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error confirming password reset", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage() != null ? e.getMessage() : "Ошибка при сбросе пароля");
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Проверяем валидность токена
            passwordResetService.cleanupExpiredTokens();
            
            boolean isValid = passwordResetService.isTokenValid(token);
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
}

