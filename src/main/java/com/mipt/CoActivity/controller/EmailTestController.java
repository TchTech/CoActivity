package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test/email")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class EmailTestController {
    private static final Logger logger = LoggerFactory.getLogger(EmailTestController.class);
    
    private final EmailService emailService;
    
    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    /**
     * Тестовый endpoint для проверки отправки email
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testEmail(@RequestParam String toEmail) {
        Map<String, String> response = new HashMap<>();
        
        try {
            logger.info("Testing email send to: {}", toEmail);
            emailService.sendSimpleEmail(toEmail, "Test Email - CoActivity", 
                "Это тестовое письмо для проверки работы почтового сервера.");
            response.put("status", "success");
            response.put("message", "Тестовое письмо отправлено на " + toEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send test email", e);
            response.put("status", "error");
            response.put("message", "Ошибка отправки: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

