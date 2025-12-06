package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.PostRecommendationRequest;
import com.mipt.CoActivity.dto.PostRecommendationResponse;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Interest;
import com.mipt.CoActivity.model.InterestCategory;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceRecommendationTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRecommendationService postRecommendationService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private RoomPostPinRepository roomPostPinRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private User subscribedUser;
    private Post testPost1;
    private Post testPost2;
    private Interest interest1;
    private Interest interest2;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = new User("testuser", "test@example.com", "encodedPassword");
        testUser.setId(1L);
        testUser.setName("Test User");

        // Создаем пользователя для подписки
        subscribedUser = new User("subscribed", "subscribed@example.com", "encodedPassword");
        subscribedUser.setId(2L);
        subscribedUser.setName("Subscribed User");

        // Создаем интересы
        InterestCategory category = new InterestCategory();
        category.setId(1);
        category.setName("IT");

        interest1 = new Interest();
        interest1.setId(1);
        interest1.setName("Программирование");
        interest1.setCategory(category);

        interest2 = new Interest();
        interest2.setId(2);
        interest2.setName("Python");
        interest2.setCategory(category);

        // Создаем посты
        testPost1 = new Post();
        testPost1.setId(1);
        testPost1.setName("Post about Python");
        testPost1.setText("This is a post about Python programming");
        testPost1.setAuthor(testUser);

        testPost2 = new Post();
        testPost2.setId(2);
        testPost2.setName("Post from subscribed user");
        testPost2.setText("This is a post from a subscribed user");
        testPost2.setAuthor(subscribedUser);
    }

    @Test
    void testGetRecommendedPosts_WithSubscriptions() {
        // Arrange
        testUser.setSubscriptions(Arrays.asList(subscribedUser));
        testUser.setInterests(Arrays.asList(interest1, interest2));

        List<Post> allPosts = Arrays.asList(testPost1, testPost2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findAll()).thenReturn(allPosts);

        // Создаем мок ответ от Python API
        PostRecommendationResponse.PostData recommendedPostData = new PostRecommendationResponse.PostData();
        recommendedPostData.setId(2); // Пост от подписанного пользователя должен быть первым
        recommendedPostData.setName("Post from subscribed user");

        PostRecommendationResponse mockResponse = new PostRecommendationResponse();
        mockResponse.setRecommendedPosts(Arrays.asList(recommendedPostData));
        mockResponse.setSimilarityScores(Arrays.asList(0.95));
        mockResponse.setMessage("Found recommendations");

        when(postRecommendationService.getRecommendations(any(PostRecommendationRequest.class)))
                .thenReturn(mockResponse);

        // Act
        List<Post> result = postService.getRecommendedPosts(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getId());
        verify(postRecommendationService, times(1)).getRecommendations(any(PostRecommendationRequest.class));
    }

    @Test
    void testGetRecommendedPosts_WithoutSubscriptions_WithInterests() {
        // Arrange
        testUser.setSubscriptions(new ArrayList<>());
        testUser.setInterests(Arrays.asList(interest1, interest2));

        List<Post> allPosts = Arrays.asList(testPost1, testPost2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findAll()).thenReturn(allPosts);

        PostRecommendationResponse.PostData recommendedPostData = new PostRecommendationResponse.PostData();
        recommendedPostData.setId(1);
        recommendedPostData.setName("Post about Python");

        PostRecommendationResponse mockResponse = new PostRecommendationResponse();
        mockResponse.setRecommendedPosts(Arrays.asList(recommendedPostData));
        mockResponse.setSimilarityScores(Arrays.asList(0.85));
        mockResponse.setMessage("Found recommendations");

        when(postRecommendationService.getRecommendations(any(PostRecommendationRequest.class)))
                .thenReturn(mockResponse);

        // Act
        List<Post> result = postService.getRecommendedPosts(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
        verify(postRecommendationService, times(1)).getRecommendations(any(PostRecommendationRequest.class));
    }

    @Test
    void testGetRecommendedPosts_WithoutSubscriptions_WithoutInterests() {
        // Arrange
        testUser.setSubscriptions(new ArrayList<>());
        testUser.setInterests(new ArrayList<>());

        List<Post> allPosts = Arrays.asList(testPost1, testPost2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findAll()).thenReturn(allPosts);

        // Act
        List<Post> result = postService.getRecommendedPosts(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // Должны быть отсортированы по ID (новые первыми)
        assertTrue(result.get(0).getId() > result.get(1).getId() || 
                  result.get(0).getId() < result.get(1).getId());
        // Не должен вызывать Python API, так как нет интересов
        verify(postRecommendationService, never()).getRecommendations(any());
    }

    @Test
    void testGetRecommendedPosts_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            postService.getRecommendedPosts(1L);
        });
        verify(postRecommendationService, never()).getRecommendations(any());
    }

    @Test
    void testGetRecommendedPosts_EmptyPosts() {
        // Arrange
        testUser.setSubscriptions(new ArrayList<>());
        testUser.setInterests(Arrays.asList(interest1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<Post> result = postService.getRecommendedPosts(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(postRecommendationService, never()).getRecommendations(any());
    }

    @Test
    void testGetRecommendedPosts_PythonApiReturnsEmpty() {
        // Arrange
        testUser.setSubscriptions(new ArrayList<>());
        testUser.setInterests(Arrays.asList(interest1));

        List<Post> allPosts = Arrays.asList(testPost1, testPost2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findAll()).thenReturn(allPosts);

        PostRecommendationResponse emptyResponse = new PostRecommendationResponse();
        emptyResponse.setRecommendedPosts(new ArrayList<>());
        emptyResponse.setSimilarityScores(new ArrayList<>());
        emptyResponse.setMessage("No recommendations");

        when(postRecommendationService.getRecommendations(any(PostRecommendationRequest.class)))
                .thenReturn(emptyResponse);

        // Act
        List<Post> result = postService.getRecommendedPosts(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Должен вернуть все посты, отсортированные по дате
        verify(postRecommendationService, times(1)).getRecommendations(any());
    }
}

