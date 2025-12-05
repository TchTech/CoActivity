package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TwoFactorService {
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorService.class);

    private final UserRepository userRepository;
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeGenerator codeGenerator;

    @Value("${app.name:CoActivity}")
    private String appName;

    public TwoFactorService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.codeGenerator = new DefaultCodeGenerator();
    }

    /**
     * Генерирует секретный ключ для 2FA и создает QR-код
     */
    public TwoFactorSetupResult generateSecretAndQrCode(User user) {
        String secret = secretGenerator.generate();
        
        String qrCodeImageUri;
        try {
            QrData qrData = new QrData.Builder()
                    .label(user.getEmail())
                    .secret(secret)
                    .issuer(appName)
                    .algorithm(HashingAlgorithm.SHA1)
                    .digits(6)
                    .period(30)
                    .build();
            
            byte[] qrCodeImage = qrGenerator.generate(qrData);
            qrCodeImageUri = Utils.getDataUriForImage(qrCodeImage, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            logger.error("Error generating QR code for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to generate QR code", e);
        }

        // Форматируем секретный ключ для ручного ввода (пробелы каждые 4 символа)
        String manualEntryKey = formatSecretForDisplay(secret);

        return new TwoFactorSetupResult(secret, qrCodeImageUri, manualEntryKey);
    }

    /**
     * Проверяет код TOTP для пользователя
     */
    public boolean verifyCode(User user, String code) {
        if (user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isEmpty()) {
            logger.warn("Attempted to verify 2FA code for user {} without a secret", user.getEmail());
            return false;
        }

        return verifyCodeWithSecret(user.getTwoFactorSecret(), code);
    }

    /**
     * Проверяет код TOTP по секретному ключу (для настройки)
     */
    public boolean verifyCodeWithSecret(String secret, String code) {
        if (secret == null || secret.isEmpty() || code == null || code.length() != 6) {
            return false;
        }
        
        try {
            // Проверяем текущий временной интервал и соседние (для задержек)
            long currentTimeSeconds = Instant.now().getEpochSecond();
            long timeStep = 30; // 30 секунд
            
            // Проверяем текущий, предыдущий и следующий интервалы (допускаем ±30 секунд)
            for (int i = -1; i <= 1; i++) {
                long timeStepValue = (currentTimeSeconds / timeStep) + i;
                String expectedCode = codeGenerator.generate(secret, timeStepValue);
                
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error verifying 2FA code", e);
            return false;
        }
    }

    /**
     * Включает 2FA для пользователя
     */
    @Transactional
    public void enableTwoFactor(User user, String secret) {
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        logger.info("2FA enabled for user: {}", user.getEmail());
    }

    /**
     * Отключает 2FA для пользователя
     */
    @Transactional
    public void disableTwoFactor(User user) {
        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        logger.info("2FA disabled for user: {}", user.getEmail());
    }

    /**
     * Форматирует секретный ключ для удобного отображения (пробелы каждые 4 символа)
     */
    private String formatSecretForDisplay(String secret) {
        if (secret == null || secret.length() <= 4) {
            return secret;
        }
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secret.length(); i += 4) {
            if (i > 0) {
                formatted.append(" ");
            }
            int end = Math.min(i + 4, secret.length());
            formatted.append(secret.substring(i, end));
        }
        return formatted.toString();
    }

    /**
     * Внутренний класс для результата настройки 2FA
     */
    public static class TwoFactorSetupResult {
        private final String secret;
        private final String qrCodeImageUri;
        private final String manualEntryKey;

        public TwoFactorSetupResult(String secret, String qrCodeImageUri, String manualEntryKey) {
            this.secret = secret;
            this.qrCodeImageUri = qrCodeImageUri;
            this.manualEntryKey = manualEntryKey;
        }

        public String getSecret() {
            return secret;
        }

        public String getQrCodeImageUri() {
            return qrCodeImageUri;
        }

        public String getManualEntryKey() {
            return manualEntryKey;
        }
    }
}
