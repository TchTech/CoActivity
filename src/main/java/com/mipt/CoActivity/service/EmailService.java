package com.mipt.CoActivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

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
        logger.info("=== Starting password reset email send ===");
        logger.info("To: {}, From: {}, Host: {}, Port: {}, BaseUrl: {}", toEmail, fromEmail, mailHost, mailPort, passwordResetBaseUrl);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Сброс пароля - CoActivity");
            
            String resetLink = passwordResetBaseUrl + "/reset-password?token=" + token;
            logger.info("Password reset link generated: {}", resetLink);
            
            String emailBody = buildEmailTemplate(
                "Сброс пароля",
                "Вы запросили сброс пароля для вашей учетной записи CoActivity.",
                "Для сброса пароля нажмите на кнопку ниже:",
                resetLink,
                "Сбросить пароль",
                "Эта ссылка действительна в течение 24 часов.",
                "Если вы не запрашивали сброс пароля, просто проигнорируйте это письмо."
            );
            
            helper.setText(emailBody, true);
            addLogoToEmail(helper);
            
            logger.info("Attempting to send password reset email via mailSender...");
            mailSender.send(message);
            
            logger.info("✓ Password reset email successfully sent to: {}", toEmail);
            logger.info("=== Password reset email send completed successfully ===");
        } catch (MessagingException e) {
            logger.error("✗✗✗ FAILED to send password reset email to: {}", toEmail);
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            logger.error("Email configuration - from: {}, baseUrl: {}, host: {}, port: {}", 
                        fromEmail, passwordResetBaseUrl, mailHost, mailPort);
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            logger.error("Full stack trace:", e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }
    
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Конвертируем plain text в HTML с базовым форматированием
            addLogoToEmail(helper);
            String htmlBody = buildSimpleEmailTemplate(text);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            logger.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    public void sendEmailVerificationEmail(String toEmail, String token) {
        logger.info("=== Starting email verification email send ===");
        logger.info("To: {}, From: {}, Host: {}, Port: {}, BaseUrl: {}", toEmail, fromEmail, mailHost, mailPort, emailVerificationBaseUrl);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Подтверждение email - CoActivity");
            
            String verificationLink = emailVerificationBaseUrl + "/verify-email?token=" + token;
            logger.info("Verification link generated: {}", verificationLink);
            
            String emailBody = buildEmailTemplate(
                "Подтверждение email",
                "Благодарим за регистрацию в CoActivity!",
                "Для завершения регистрации и активации вашего аккаунта, пожалуйста, подтвердите ваш email, нажав на кнопку ниже:",
                verificationLink,
                "Подтвердить email",
                "Эта ссылка действительна в течение 24 часов.",
                "Если вы не регистрировались в CoActivity, просто проигнорируйте это письмо."
            );
            
            addLogoToEmail(helper);
            helper.setText(emailBody, true);
            
            logger.info("Attempting to send email via mailSender...");
            mailSender.send(message);
            
            logger.info("✓ Email verification email successfully sent to: {}", toEmail);
            logger.info("=== Email send completed successfully ===");
        } catch (MessagingException e) {
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Подтверждение email - CoActivity");
            
            String verificationLink = emailVerificationBaseUrl + "/verify-email?token=" + token;
            String emailBody = buildEmailTemplate(
                "Подтверждение email",
                "Вы запросили повторную отправку письма для подтверждения email.",
                "Для подтверждения вашего email нажмите на кнопку ниже:",
                verificationLink,
                "Подтвердить email",
                "Эта ссылка действительна в течение 24 часов.",
                "Если вы не запрашивали это письмо, просто проигнорируйте его."
            );
            
            helper.setText(emailBody, true);
            addLogoToEmail(helper);
            mailSender.send(message);
            
            logger.info("Email verification resend email sent to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send email verification resend email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Создает HTML-шаблон письма в стиле платформы CoActivity
     */
    private String buildEmailTemplate(String title, String greeting, String mainText, String actionLink, String actionButtonText, String warningText, String footerText) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ru\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "    <title>" + title + " - CoActivity</title>\n" +
                "    <style>\n" +
                "        * {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "        body {\n" +
                "            font-family: 'Roboto', -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;\n" +
                "            background-color: #1a1a1a;\n" +
                "            color: #e0e0e0;\n" +
                "            line-height: 1.6;\n" +
                "            -webkit-font-smoothing: antialiased;\n" +
                "            -moz-osx-font-smoothing: grayscale;\n" +
                "        }\n" +
                "        .email-container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "            background-color: #121212;\n" +
                "        }\n" +
                "        .email-header {\n" +
                "            background: linear-gradient(135deg, #d4af37 0%, #c9a86b 100%);\n" +
                "            padding: 40px 30px;\n" +
                "            text-align: center;\n" +
                "            border-radius: 0;\n" +
                "        }\n" +
                "        .email-logo {\n" +
                "            font-size: 32px;\n" +
                "            font-weight: 700;\n" +
                "            color: #121212;\n" +
                "            letter-spacing: 2px;\n" +
                "            text-transform: uppercase;\n" +
                "            margin: 0;\n" +
                "        }\n" +
                "        .email-content {\n" +
                "            padding: 40px 30px;\n" +
                "        }\n" +
                "        .email-title {\n" +
                "            font-size: 24px;\n" +
                "            font-weight: 600;\n" +
                "            color: #d4af37;\n" +
                "            margin-bottom: 20px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .email-greeting {\n" +
                "            font-size: 16px;\n" +
                "            color: #e0e0e0;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .email-text {\n" +
                "            font-size: 16px;\n" +
                "            color: #e0e0e0;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .email-button-container {\n" +
                "            text-align: center;\n" +
                "            margin: 35px 0;\n" +
                "        }\n" +
                "        .email-button {\n" +
                "            display: inline-block;\n" +
                "            padding: 16px 40px;\n" +
                "            background: linear-gradient(135deg, #d4af37 0%, #c9a86b 100%);\n" +
                "            color: #121212;\n" +
                "            text-decoration: none;\n" +
                "            border-radius: 8px;\n" +
                "            font-weight: 600;\n" +
                "            font-size: 16px;\n" +
                "            transition: all 0.3s ease;\n" +
                "            box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3);\n" +
                "        }\n" +
                "        .email-button:hover {\n" +
                "            transform: translateY(-2px);\n" +
                "            box-shadow: 0 6px 16px rgba(212, 175, 55, 0.4);\n" +
                "        }\n" +
                "        .email-link {\n" +
                "            color: #d4af37;\n" +
                "            word-break: break-all;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .email-warning {\n" +
                "            background-color: #242424;\n" +
                "            border-left: 4px solid #d4af37;\n" +
                "            padding: 15px 20px;\n" +
                "            margin: 25px 0;\n" +
                "            border-radius: 4px;\n" +
                "            font-size: 14px;\n" +
                "            color: #e0e0e0;\n" +
                "        }\n" +
                "        .email-footer {\n" +
                "            background-color: #1a1a1a;\n" +
                "            padding: 30px;\n" +
                "            text-align: center;\n" +
                "            border-top: 1px solid #333333;\n" +
                "        }\n" +
                "        .email-footer-text {\n" +
                "            font-size: 14px;\n" +
                "            color: #888888;\n" +
                "            margin-bottom: 15px;\n" +
                "            line-height: 1.6;\n" +
                "        }\n" +
                "        .email-footer-signature {\n" +
                "            font-size: 16px;\n" +
                "            color: #d4af37;\n" +
                "            font-weight: 600;\n" +
                "            margin-top: 20px;\n" +
                "        }\n" +
                "        .email-divider {\n" +
                "            height: 1px;\n" +
                "            background-color: #333333;\n" +
                "            margin: 30px 0;\n" +
                "        }\n" +
                "        @media only screen and (max-width: 600px) {\n" +
                "            .email-content {\n" +
                "                padding: 30px 20px;\n" +
                "            }\n" +
                "            .email-header {\n" +
                "                padding: 30px 20px;\n" +
                "            }\n" +
                "            .email-title {\n" +
                "                font-size: 20px;\n" +
                "            }\n" +
                "            .email-button {\n" +
                "                padding: 14px 30px;\n" +
                "                font-size: 15px;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"email-header\">\n" +
                "            <img src=\"cid:logo\" alt=\"CoActivity\" style=\"max-width: 300px; height: auto; margin-bottom: 10px; display: block; margin-left: auto; margin-right: auto;\" />\n" +
                "            <h1 class=\"email-logo\" style=\"margin-top: 10px;\">CoActivity</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-content\">\n" +
                "            <h2 class=\"email-title\">" + title + "</h2>\n" +
                "            <p class=\"email-greeting\">Здравствуйте!</p>\n" +
                "            <p class=\"email-text\">" + greeting + "</p>\n" +
                "            <p class=\"email-text\">" + mainText + "</p>\n" +
                "            <div class=\"email-button-container\">\n" +
                "                <a href=\"" + actionLink + "\" class=\"email-button\">" + actionButtonText + "</a>\n" +
                "            </div>\n" +
                "            <div class=\"email-warning\">\n" +
                "                ⏰ " + warningText + "\n" +
                "            </div>\n" +
                "            <div class=\"email-divider\"></div>\n" +
                "            <p class=\"email-text\" style=\"font-size: 14px; color: #888888;\">\n" +
                "                Если кнопка не работает, скопируйте и вставьте следующую ссылку в браузер:<br>\n" +
                "                <a href=\"" + actionLink + "\" class=\"email-link\">" + actionLink + "</a>\n" +
                "            </p>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p class=\"email-footer-text\">" + footerText + "</p>\n" +
                "            <p class=\"email-footer-signature\">С уважением,<br>Команда CoActivity</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Добавляет логотип CoActivity в письмо как встроенное изображение
     */
    private void addLogoToEmail(MimeMessageHelper helper) {
        try {
            Resource logoResource = new ClassPathResource("images/CoActivity.png");
            if (logoResource.exists()) {
                helper.addInline("logo", logoResource);
                logger.debug("Logo image added to email");
            } else {
                logger.warn("Logo image not found at images/CoActivity.png, skipping logo in email");
            }
        } catch (MessagingException e) {
            logger.warn("Failed to add logo to email: {}", e.getMessage());
            // Не прерываем отправку письма, если не удалось добавить логотип
        } catch (Exception e) {
            logger.warn("Unexpected error adding logo to email: {}", e.getMessage());
            // Не прерываем отправку письма, если не удалось добавить логотип
        }
    }

    /**
     * Создает простой HTML-шаблон для текстовых уведомлений
     */
    private String buildSimpleEmailTemplate(String text) {
        // Конвертируем переносы строк в HTML-теги
        String htmlText = text.replace("\n\n", "</p><p class=\"email-text\">")
                               .replace("\n", "<br>");
        
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ru\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body {\n" +
                "            font-family: 'Roboto', -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;\n" +
                "            background-color: #1a1a1a;\n" +
                "            color: #e0e0e0;\n" +
                "            line-height: 1.6;\n" +
                "        }\n" +
                "        .email-container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "            background-color: #121212;\n" +
                "        }\n" +
                "        .email-header {\n" +
                "            background: linear-gradient(135deg, #d4af37 0%, #c9a86b 100%);\n" +
                "            padding: 30px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .email-logo {\n" +
                "            font-size: 28px;\n" +
                "            font-weight: 700;\n" +
                "            color: #121212;\n" +
                "            letter-spacing: 2px;\n" +
                "            text-transform: uppercase;\n" +
                "        }\n" +
                "        .email-content {\n" +
                "            padding: 30px;\n" +
                "        }\n" +
                "        .email-text {\n" +
                "            font-size: 16px;\n" +
                "            color: #e0e0e0;\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .email-footer {\n" +
                "            background-color: #1a1a1a;\n" +
                "            padding: 20px;\n" +
                "            text-align: center;\n" +
                "            border-top: 1px solid #333333;\n" +
                "            font-size: 14px;\n" +
                "            color: #888888;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"email-header\">\n" +
                "            <img src=\"cid:logo\" alt=\"CoActivity\" style=\"max-width: 300px; height: auto; margin-bottom: 10px; display: block; margin-left: auto; margin-right: auto;\" />\n" +
                "            <h1 class=\"email-logo\" style=\"margin-top: 10px;\">CoActivity</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-content\">\n" +
                "            <p class=\"email-text\">" + htmlText + "</p>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p>С уважением, команда CoActivity</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}

