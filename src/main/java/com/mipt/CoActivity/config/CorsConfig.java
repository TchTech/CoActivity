package com.mipt.CoActivity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Разрешаем запросы с различных localhost URL (Next.js frontend)
        List<String> allowedOrigins = Arrays.asList(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3001",
            "http://[::1]:3000",
            "http://[::1]:3001"
        );
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Разрешаем все методы
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Разрешаем все заголовки
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Разрешаем отправку cookies и credentials
        configuration.setAllowCredentials(true);
        
        // Разрешаем экспозицию заголовков в ответе
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count"));
        
        // Максимальное время кеширования preflight запросов
        configuration.setMaxAge(3600L);
        
        logger.info("CORS configuration initialized with allowed origins: {}", allowedOrigins);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsFilter filter = new CorsFilter(corsConfigurationSource());
        logger.info("CORS filter initialized");
        return filter;
    }
}

