package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.LoginRequest;
import com.mipt.CoActivity.dto.LoginResponse;
import com.mipt.CoActivity.dto.RegisterRequest;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.UnauthorizedException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, 
                          BCryptPasswordEncoder passwordEncoder,
                          EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    @Transactional
    public User registerNewUser(RegisterRequest request) {
        // Валидация входных данных
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Name cannot be empty");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email cannot be empty");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Password cannot be empty");
        }
        if (request.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new BadRequestException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = new User(request.getName(), request.getEmail(), hashedPassword);
        newUser.setName(request.getName());
        // Устанавливаем emailVerified = false при регистрации (по умолчанию уже false)
        newUser.setEmailVerified(false);
        
        User savedUser = userRepository.save(newUser);
        
        // Отправляем письмо подтверждения email
        try {
            logger.info("Sending verification email to newly registered user: {}", savedUser.getEmail());
            emailVerificationService.sendVerificationEmail(savedUser);
            logger.info("Verification email sent successfully to user: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send verification email to user: {}", savedUser.getEmail(), e);
            // Не блокируем регистрацию, но логируем ошибку
            // Пользователь сможет запросить повторную отправку через /email-verification/resend
        }
        
        return savedUser;
    }

    @Override
    public LoginResponse loginUser(LoginRequest request) {
        // Поддержка входа по email или username
        User user = null;
        String login = request.getLogin(); // Используем поле login
        
        if (login == null || login.trim().isEmpty()) {
            throw new BadRequestException("Login cannot be empty");
        }
        
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Password cannot be empty");
        }
        
        // Определяем, является ли login email или username
        boolean isEmail = login.contains("@");
        if (isEmail) {
            user = userRepository.findByEmail(login);
        } else {
            user = userRepository.findByUsername(login);
        }
        
        if (user == null) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Проверяем, что у пользователя есть хэш пароля
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Проверяем, подтвержден ли email (обязательное условие для входа)
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            logger.warn("Login attempt for unverified email: {}", user.getEmail());
            throw new UnauthorizedException("Email not verified. Please check your email and verify your account before logging in.");
        }

        // Проверяем, включена ли 2FA
        Boolean requiresTwoFactor = Boolean.TRUE.equals(user.getTwoFactorEnabled());
        
        // Если 2FA включена, не возвращаем токен - фронтенд запросит код
        String token = requiresTwoFactor ? null : generateToken(user);
        
        return LoginResponse.builder()
            .token(token)
            .userId(user.getId())
            .requiresTwoFactor(requiresTwoFactor)
            .build();
    }

    private String generateToken(User user) {
        // TODO: Implement JWT token generation
        // For now, return a placeholder token
        return "placeholder-jwt-token-" + user.getId();
    }
}

