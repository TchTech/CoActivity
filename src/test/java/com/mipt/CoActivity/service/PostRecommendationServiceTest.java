package com.mipt.CoActivity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.PostRecommendationRequest;
import com.mipt.CoActivity.dto.PostRecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostRecommendationServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PostRecommendationService postRecommendationService;

    private PostRecommendationRequest request;
    private PostRecommendationResponse response;

    @BeforeEach
    void setUp() throws Exception {
        request = new PostRecommendationRequest();
        response = new PostRecommendationResponse(new ArrayList<>(), new ArrayList<>(), null);

        ReflectionTestUtils.setField(postRecommendationService, "recommendationApiUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(postRecommendationService, "timeoutMillis", 5000);
        ReflectionTestUtils.setField(postRecommendationService, "httpClient", httpClient);
        ReflectionTestUtils.setField(postRecommendationService, "objectMapper", objectMapper);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    }

    @Test
    void testGetRecommendations_Success() throws Exception {
        // Given
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"recommendedPosts\":[]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(response);

        // When
        PostRecommendationResponse result = postRecommendationService.getRecommendations(request);

        // Then
        assertNotNull(result);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testGetRecommendations_ReturnsEmptyOnError() throws Exception {
        // Given
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        // When
        PostRecommendationResponse result = postRecommendationService.getRecommendations(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMessage());
    }

    @Test
    void testGetRecommendations_HandlesTimeout() throws Exception {
        // Given
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.net.http.HttpTimeoutException("Timeout"));

        // When
        PostRecommendationResponse result = postRecommendationService.getRecommendations(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMessage());
    }

    @Test
    void testGetRecommendations_HandlesGenericException() throws Exception {
        // Given
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When
        PostRecommendationResponse result = postRecommendationService.getRecommendations(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMessage());
    }
}
