package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Comment;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.CommentRepository;
import com.mipt.CoActivity.repository.PostRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    private Post post;
    private User postAuthor;
    private User commentAuthor;
    private Comment comment;
    private Comment parentComment;

    @BeforeEach
    void setUp() {
        postAuthor = new User("postauthor", "postauthor@test.com", "password");
        postAuthor.setId(1L);
        postAuthor.setName("Post Author");

        commentAuthor = new User("commentauthor", "commentauthor@test.com", "password");
        commentAuthor.setId(2L);
        commentAuthor.setName("Comment Author");

        post = new Post();
        post.setId(1);
        post.setName("Test Post");
        post.setAuthor(postAuthor);

        comment = new Comment();
        comment.setText("Test comment");
        comment.setAuthor(commentAuthor);
        comment.setId(1L);

        parentComment = new Comment();
        parentComment.setId(2L);
        parentComment.setAuthor(postAuthor);
        parentComment.setText("Parent comment");
    }

    @Test
    void testCreateComment_SendsNotificationToPostAuthor() {
        // Given
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        Comment result = commentService.createComment(1L, comment);

        // Then
        assertNotNull(result);
        verify(notificationService, atLeastOnce()).createNotification(
            eq(1L), // post author ID
            eq("POST_COMMENTED"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testCreateComment_DoesNotNotifyWhenAuthorCommentsOwnPost() {
        // Given
        comment.setAuthor(postAuthor);
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(postAuthor));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.createComment(1L, comment);

        // Then
        verify(notificationService, never()).createNotification(
            eq(1L),
            eq("POST_COMMENTED"),
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    void testCreateComment_WithReply_SendsNotificationToParentCommentAuthor() {
        // Given
        comment.setParentComment(parentComment);
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.createComment(1L, comment);

        // Then
        verify(notificationService, atLeastOnce()).createNotification(
            eq(1L), // parent comment author ID
            eq("COMMENT_REPLY"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testCreateComment_WithMentions_SendsMentionNotifications() {
        // Given
        comment.setText("Hello @postauthor");
        User mentionedUser = new User("postauthor", "postauthor@test.com", "password");
        mentionedUser.setId(3L);
        mentionedUser.setName("Post Author");

        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(userRepository.findByUsername("postauthor")).thenReturn(mentionedUser);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.createComment(1L, comment);

        // Then
        verify(notificationService, atLeastOnce()).createNotification(
            eq(3L), // mentioned user ID
            eq("MENTION"),
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void testCreateComment_ThrowsExceptionWhenPostNotFound() {
        // Given
        when(postRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            commentService.createComment(1L, comment);
        });
    }

    @Test
    void testAddComment_CreatesCommentSuccessfully() {
        // Given
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        Comment result = commentService.addComment(1L, 2L, "Test comment");

        // Then
        assertNotNull(result);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void testGetComments_ReturnsRootCommentsWithReplies() {
        // Given
        Comment reply = new Comment();
        reply.setId(3L);
        reply.setParentComment(comment);
        
        when(commentRepository.findByPostIdAndParentCommentIsNull(1)).thenReturn(List.of(comment));
        when(commentRepository.findByParentCommentId(1L)).thenReturn(List.of(reply));

        // When
        List<Comment> result = commentService.getComments(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(comment.getId(), result.get(0).getId());
        verify(commentRepository, times(1)).findByPostIdAndParentCommentIsNull(1);
    }

    @Test
    void testDeleteComment_Success() {
        // Given
        comment.setLikedUsers(new ArrayList<>());
        comment.setDislikedUsers(new ArrayList<>());
        comment.setReplies(new ArrayList<>());
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.deleteComment(1L, 2L);

        // Then
        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void testDeleteComment_ThrowsExceptionWhenNotAuthor() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commentService.deleteComment(1L, 999L);
        });
    }

    @Test
    void testDeleteComment_DeletesReplies() {
        // Given
        Comment reply = new Comment();
        reply.setId(3L);
        reply.setLikedUsers(new ArrayList<>());
        reply.setDislikedUsers(new ArrayList<>());
        comment.setReplies(new ArrayList<>());
        comment.getReplies().add(reply);
        comment.setLikedUsers(new ArrayList<>());
        comment.setDislikedUsers(new ArrayList<>());
        
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.deleteComment(1L, 2L);

        // Then
        verify(commentRepository, times(1)).deleteAll(comment.getReplies());
        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void testAddOrRemoveLike_AddsLike() {
        // Given
        comment.setLikedUsers(new ArrayList<>());
        comment.setDislikedUsers(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.addOrRemoveLike(2L, 1L);

        // Then
        assertTrue(comment.getLikedUsers().contains(commentAuthor));
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    void testAddOrRemoveLike_RemovesLike() {
        // Given
        comment.setLikedUsers(new ArrayList<>());
        comment.setDislikedUsers(new ArrayList<>());
        comment.getLikedUsers().add(commentAuthor);
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.addOrRemoveLike(2L, 1L);

        // Then
        assertFalse(comment.getLikedUsers().contains(commentAuthor));
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    void testAddOrRemoveDislike_AddsDislike() {
        // Given
        comment.setLikedUsers(new ArrayList<>());
        comment.setDislikedUsers(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.addOrRemoveDislike(2L, 1L);

        // Then
        assertTrue(comment.getDislikedUsers().contains(commentAuthor));
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    void testAddOrRemoveDislike_RemovesDislike() {
        // Given
        comment.setLikedUsers(new ArrayList<>());
        comment.setDislikedUsers(new ArrayList<>());
        comment.getDislikedUsers().add(commentAuthor);
        when(userRepository.findById(2L)).thenReturn(Optional.of(commentAuthor));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        commentService.addOrRemoveDislike(2L, 1L);

        // Then
        assertFalse(comment.getDislikedUsers().contains(commentAuthor));
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    void testCreateComment_ThrowsExceptionWhenAuthorIsNull() {
        // Given
        comment.setAuthor(null);
        when(postRepository.findById(1)).thenReturn(Optional.of(post));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commentService.createComment(1L, comment);
        });
    }

    @Test
    void testAddComment_ThrowsExceptionWhenUserNotFound() {
        // Given
        when(postRepository.findById(1)).thenReturn(Optional.of(post));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            commentService.addComment(1L, 999L, "Test");
        });
    }
}

