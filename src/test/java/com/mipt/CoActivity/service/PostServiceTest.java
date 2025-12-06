package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private RoomPostPinRepository roomPostPinRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PostService postService;

    private User author;
    private User follower;
    private Post post;
    private Room room;

    @BeforeEach
    void setUp() {
        author = new User("author", "author@test.com", "password");
        author.setId(1L);
        author.setName("Author Name");
        author.setFollowers(new ArrayList<>());

        follower = new User("follower", "follower@test.com", "password");
        follower.setId(2L);
        follower.setName("Follower Name");
        author.getFollowers().add(follower);

        post = new Post();
        post.setId(1);
        post.setName("Test Post");
        post.setText("Test content");
        post.setAuthor(author);

        room = new Room();
        room.setId(1L);
        room.setName("Test Room");
        room.setCollaborators(new ArrayList<>());
        room.getCollaborators().add(author);
    }

    @Test
    void testCreatePost_SendsNotificationsToFollowers() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        Post result = postService.createPost(post);

        // Then
        assertNotNull(result);
        verify(notificationService, atLeastOnce()).createNotification(
            eq(2L), // follower ID
            eq("NEW_POST"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testCreatePost_DoesNotNotifyAuthor() {
        // Given
        author.getFollowers().clear(); // No followers
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        postService.createPost(post);

        // Then
        verify(notificationService, never()).createNotification(
            eq(1L), // author ID
            eq("NEW_POST"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testCreatePost_WithMentions_SendsMentionNotifications() {
        // Given
        post.setText("Hello @follower and @otheruser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(userRepository.findByUsername("follower")).thenReturn(follower);
        when(userRepository.findByUsername("otheruser")).thenReturn(null);
        when(userRepository.findAll()).thenReturn(List.of(follower));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        postService.createPost(post);

        // Then
        verify(notificationService, atLeastOnce()).createNotification(
            eq(2L), // follower ID
            eq("MENTION"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testAddOrRemoveLike_SendsNotificationToPostAuthor() {
        // Given
        User liker = new User("liker", "liker@test.com", "password");
        liker.setId(3L);
        liker.setName("Liker Name");
        post.setLikedUsers(new ArrayList<>());
        post.setDislikedUsers(new ArrayList<>());

        when(userRepository.findById(3L)).thenReturn(Optional.of(liker));
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        postService.addOrRemoveLike(3L, 1L);

        // Then
        verify(notificationService, atLeastOnce()).createNotification(
            eq(1L), // post author ID
            eq("POST_LIKED"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testAddOrRemoveLike_DoesNotNotifyWhenAuthorLikesOwnPost() {
        // Given
        post.setLikedUsers(new ArrayList<>());
        post.setDislikedUsers(new ArrayList<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        postService.addOrRemoveLike(1L, 1L);

        // Then
        verify(notificationService, never()).createNotification(
            eq(1L),
            eq("POST_LIKED"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testAddOrRemoveDislike_SendsNotificationToPostAuthor() {
        // Given
        User disliker = new User("disliker", "disliker@test.com", "password");
        disliker.setId(3L);
        disliker.setName("Disliker Name");
        post.setLikedUsers(new ArrayList<>());
        post.setDislikedUsers(new ArrayList<>());

        when(userRepository.findById(3L)).thenReturn(Optional.of(disliker));
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        postService.addOrRemoveDislike(3L, 1L);

        // Then
        verify(notificationService, atLeastOnce()).createNotification(
            eq(1L), // post author ID
            eq("POST_DISLIKED"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testAddOrRemoveDislike_DoesNotNotifyWhenAuthorDislikesOwnPost() {
        // Given
        post.setLikedUsers(new ArrayList<>());
        post.setDislikedUsers(new ArrayList<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        postService.addOrRemoveDislike(1L, 1L);

        // Then
        verify(notificationService, never()).createNotification(
            eq(1L),
            eq("POST_DISLIKED"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testCreatePost_ThrowsExceptionWhenAuthorNotFound() {
        // Given
        post.setAuthor(new User());
        post.getAuthor().setId(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            postService.createPost(post);
        });
    }
}

