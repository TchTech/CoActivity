package com.mipt.CoActivity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    
    // Разрешаем запросы с localhost:3000 (Next.js frontend)
    config.addAllowedOrigin("http://localhost:3000");
    config.addAllowedOrigin("http://127.0.0.1:3000");
    
    // Разрешаем все методы
    config.addAllowedMethod("*");
    
    // Разрешаем все заголовки
    config.addAllowedHeader("*");
    
    // Разрешаем отправку cookies
    config.setAllowCredentials(true);
    
    // Применяем конфигурацию ко всем путям
    source.registerCorsConfiguration("/**", config);
    
    return new CorsFilter(source);
  }
}

