package com.mipt.CoActivity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration.
 * 
 * Currently allows all requests without authentication.
 * This can be updated later to add proper authentication/authorization.
 * 
 * Note: CORS is handled by CorsConfig.java separately.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Disable CSRF for API endpoints (can be enabled later if needed)
        .csrf(csrf -> csrf.disable())
        
        // Enable CORS - will use CorsConfig bean
        .cors(cors -> {})
        
        // Configure session management
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        
        // Allow all requests (no authentication required)
        // This can be updated later to require authentication for specific endpoints
        .authorizeHttpRequests(auth -> auth
            // Allow all notification endpoints
            .requestMatchers("/api/notifications/**").permitAll()
            // Allow all other API endpoints
            .requestMatchers("/api/**").permitAll()
            // Allow auth endpoints
            .requestMatchers("/auth/**").permitAll()
            // Allow all other requests
            .anyRequest().permitAll()
        );

    return http.build();
  }
}

