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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostRecommendationServiceTest {

    @Mock
    private HttpClient httpClient;

    @InjectMocks
    private PostRecommendationService postRecommendationService;

    private ObjectMapper objectMapper;
    private PostRecommendationRequest testRequest;
    private PostRecommendationResponse testResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(postRecommendationService, "recommendationApiUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(postRecommendationService, "timeoutMillis", 5000);
        ReflectionTestUtils.setField(postRecommendationService, "httpClient", httpClient);
        ReflectionTestUtils.setField(postRecommendationService, "objectMapper", objectMapper);

        // Создаем тестовые данные
        PostRecommendationRequest.PostData postData = new PostRecommendationRequest.PostData();
        postData.setId(1);
        postData.setName("Test Post");
        postData.setText("This is a test post about programming");
        postData.setAuthorId(1);
        postData.setAuthorName("Test User");

        testRequest = new PostRecommendationRequest();
        testRequest.setUserInterests(Arrays.asList("Программирование", "IT"));
        testRequest.setPosts(Arrays.asList(postData));
        testRequest.setSubscribedUserIds(Arrays.asList(1));

        PostRecommendationResponse.PostData recommendedPost = new PostRecommendationResponse.PostData();
        recommendedPost.setId(1);
        recommendedPost.setName("Test Post");

        testResponse = new PostRecommendationResponse();
        testResponse.setRecommendedPosts(Arrays.asList(recommendedPost));
        testResponse.setSimilarityScores(Arrays.asList(0.95));
        testResponse.setMessage("Found 1 recommendations");
    }

    @Test
    void testGetRecommendations_Success() throws Exception {
        // Arrange
        String responseBody = objectMapper.writeValueAsString(testResponse);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // Act
        PostRecommendationResponse result = postRecommendationService.getRecommendations(testRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRecommendedPosts());
        assertEquals(1, result.getRecommendedPosts().size());
        assertEquals(1, result.getRecommendedPosts().get(0).getId());
        assertEquals("Test Post", result.getRecommendedPosts().get(0).getName());
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void testGetRecommendations_EmptyResponse() throws Exception {
        // Arrange
        PostRecommendationResponse emptyResponse = new PostRecommendationResponse();
        emptyResponse.setRecommendedPosts(new ArrayList<>());
        emptyResponse.setSimilarityScores(new ArrayList<>());
        emptyResponse.setMessage("No recommendations");

        String responseBody = objectMapper.writeValueAsString(emptyResponse);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // Act
        PostRecommendationResponse result = postRecommendationService.getRecommendations(testRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRecommendedPosts());
        assertTrue(result.getRecommendedPosts().isEmpty());
    }

    @Test
    void testGetRecommendations_HttpError() throws Exception {
        // Arrange
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // Act
        PostRecommendationResponse result = postRecommendationService.getRecommendations(testRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRecommendedPosts());
        assertTrue(result.getRecommendedPosts().isEmpty());
        assertTrue(result.getMessage().contains("500"));
    }

    @Test
    void testGetRecommendations_Timeout() throws Exception {
        // Arrange
        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new java.net.http.HttpTimeoutException("Request timed out"));

        // Act
        PostRecommendationResponse result = postRecommendationService.getRecommendations(testRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRecommendedPosts());
        assertTrue(result.getRecommendedPosts().isEmpty());
        assertTrue(result.getMessage().contains("Таймаут"));
    }

    @Test
    void testGetRecommendations_GeneralException() throws Exception {
        // Arrange
        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new RuntimeException("Connection failed"));

        // Act
        PostRecommendationResponse result = postRecommendationService.getRecommendations(testRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRecommendedPosts());
        assertTrue(result.getRecommendedPosts().isEmpty());
        assertTrue(result.getMessage().contains("Connection failed"));
    }

    @Test
    void testGetRecommendations_MultiplePosts() throws Exception {
        // Arrange
        List<PostRecommendationResponse.PostData> recommendedPosts = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            PostRecommendationResponse.PostData post = new PostRecommendationResponse.PostData();
            post.setId(i);
            post.setName("Post " + i);
            recommendedPosts.add(post);
            scores.add(0.9 - (i * 0.1));
        }

        PostRecommendationResponse multiResponse = new PostRecommendationResponse();
        multiResponse.setRecommendedPosts(recommendedPosts);
        multiResponse.setSimilarityScores(scores);
        multiResponse.setMessage("Found 5 recommendations");

        String responseBody = objectMapper.writeValueAsString(multiResponse);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // Act
        PostRecommendationResponse result = postRecommendationService.getRecommendations(testRequest);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getRecommendedPosts().size());
        assertEquals(5, result.getSimilarityScores().size());
        assertEquals(1, result.getRecommendedPosts().get(0).getId());
        assertEquals(5, result.getRecommendedPosts().get(4).getId());
    }
}

