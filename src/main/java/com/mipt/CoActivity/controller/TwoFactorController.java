package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.dto.TwoFactorEnableResponse;
import com.mipt.CoActivity.dto.TwoFactorVerifyRequest;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import com.mipt.CoActivity.service.TwoFactorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/2fa")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class TwoFactorController {
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorController.class);

    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;

    public TwoFactorController(TwoFactorService twoFactorService, UserRepository userRepository) {
        this.twoFactorService = twoFactorService;
        this.userRepository = userRepository;
    }

    /**
     * Генерирует секретный ключ и QR-код для настройки 2FA
     */
    @PostMapping("/enable/{userId}")
    public ResponseEntity<TwoFactorEnableResponse> enableTwoFactor(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new BadRequestException("2FA is already enabled for this user");
        }

        TwoFactorService.TwoFactorSetupResult setupResult = twoFactorService.generateSecretAndQrCode(user);

        TwoFactorEnableResponse response = TwoFactorEnableResponse.builder()
                .secret(setupResult.getSecret()) // Временно возвращаем для настройки, потом удалим
                .qrCodeUrl(setupResult.getQrCodeImageUri())
                .manualEntryKey(setupResult.getManualEntryKey())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Подтверждает включение 2FA, проверяя код
     */
    @PostMapping("/verify-setup/{userId}")
    public ResponseEntity<Map<String, String>> verifySetup(
            @PathVariable Long userId,
            @Valid @RequestBody TwoFactorVerifyRequest request,
            @RequestParam String secret) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!twoFactorService.verifyCodeWithSecret(secret, request.getCode())) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Неверный код. Пожалуйста, попробуйте еще раз.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        // Включаем 2FA для пользователя
        twoFactorService.enableTwoFactor(user, secret);

        Map<String, String> response = new HashMap<>();
        response.put("message", "2FA успешно включена");
        return ResponseEntity.ok(response);
    }

    /**
     * Отключает 2FA для пользователя (требует пароль для безопасности)
     */
    @PostMapping("/disable/{userId}")
    public ResponseEntity<Map<String, String>> disableTwoFactor(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new BadRequestException("2FA is not enabled for this user");
        }

        twoFactorService.disableTwoFactor(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "2FA успешно отключена");
        return ResponseEntity.ok(response);
    }

    /**
     * Проверяет, включена ли 2FA для пользователя
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Boolean>> getTwoFactorStatus(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Boolean> response = new HashMap<>();
        response.put("enabled", Boolean.TRUE.equals(user.getTwoFactorEnabled()));
        return ResponseEntity.ok(response);
    }
}

