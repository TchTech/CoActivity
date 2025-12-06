package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.dto.LoginRequest;
import com.mipt.CoActivity.dto.LoginResponse;
import com.mipt.CoActivity.dto.RegisterRequest;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<User> registerNewUser(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received: name={}, email={}, passwordLength={}", 
            request.getName(), request.getEmail(), 
            request.getPassword() != null ? request.getPassword().length() : 0);
        
        try {
            User user = authService.registerNewUser(request);
            logger.info("User registered successfully: id={}, username={}", user.getId(), user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (Exception e) {
            logger.error("Error during registration: ", e);
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody LoginRequest request) {
        logger.info("Login request received: login={}, passwordPresent={}", 
            request != null ? request.getLogin() : "null",
            request != null && request.getPassword() != null ? "yes" : "no");
        
        // Дополнительная проверка для диагностики
        if (request == null) {
            logger.error("LoginRequest is null!");
            throw new BadRequestException("Request body is required");
        }
        
        if (request.getLogin() == null || request.getLogin().trim().isEmpty()) {
            logger.error("Login field is null or empty. Request object toString: {}", 
                request.toString());
            logger.error("Login field value: '{}', password field value present: {}", 
                request.getLogin(), 
                request.getPassword() != null);
            throw new BadRequestException("Login cannot be empty");
        }
        
        try {
            LoginResponse response = authService.loginUser(request);
            logger.info("Login successful: userId={}, requiresTwoFactor={}", 
                response.getUserId(), response.getRequiresTwoFactor());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during login: ", e);
            throw e; // Глобальный обработчик перехватит это
        }
    }
}

