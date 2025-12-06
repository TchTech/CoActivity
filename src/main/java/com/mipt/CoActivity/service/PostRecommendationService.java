package com.mipt.CoActivity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.PostRecommendationRequest;
import com.mipt.CoActivity.dto.PostRecommendationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

@Service
public class PostRecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(PostRecommendationService.class);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${app.recommendation.api.url:http://localhost:8000}")
    private String recommendationApiUrl;
    
    @Value("${app.recommendation.api.timeout:5000}")
    private int timeoutMillis;

    public PostRecommendationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Получает рекомендации постов от Python API
     * 
     * @param request Запрос с интересами пользователя и постами
     * @return Рекомендации постов с оценками схожести
     */
    public PostRecommendationResponse getRecommendations(PostRecommendationRequest request) {
        try {
            String url = recommendationApiUrl + "/recommend-posts";
            
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                PostRecommendationResponse recommendationResponse = objectMapper.readValue(
                        response.body(), 
                        PostRecommendationResponse.class
                );
                logger.info("Получено {} рекомендаций постов от Python API", 
                        recommendationResponse.getRecommendedPosts() != null ? 
                        recommendationResponse.getRecommendedPosts().size() : 0);
                return recommendationResponse;
            } else {
                logger.error("Ошибка запроса к Python API: статус {}, тело: {}", 
                        response.statusCode(), response.body());
                // Возвращаем пустой ответ при ошибке
                return new PostRecommendationResponse(new ArrayList<>(), new ArrayList<>(), 
                        "Ошибка получения рекомендаций: " + response.statusCode());
            }
        } catch (java.net.http.HttpTimeoutException e) {
            logger.error("Таймаут при запросе к Python API: {}", e.getMessage());
            return new PostRecommendationResponse(new ArrayList<>(), new ArrayList<>(), 
                    "Таймаут при получении рекомендаций");
        } catch (Exception e) {
            logger.error("Ошибка при запросе к Python API: {}", e.getMessage(), e);
            // Возвращаем пустой ответ при ошибке
            return new PostRecommendationResponse(new ArrayList<>(), new ArrayList<>(), 
                    "Ошибка получения рекомендаций: " + e.getMessage());
        }
    }
}

