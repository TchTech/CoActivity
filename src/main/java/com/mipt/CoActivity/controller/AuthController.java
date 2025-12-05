package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.dto.LoginRequest;
import com.mipt.CoActivity.dto.LoginResponse;
import com.mipt.CoActivity.dto.RegisterRequest;
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
        LoginResponse response = authService.loginUser(request);
        return ResponseEntity.ok(response);
    }
}

