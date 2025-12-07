package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Comment;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.CommentRepository;
import com.mipt.CoActivity.repository.PostRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {
  private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

  @Autowired
  private CommentRepository commentRepository;
  @Autowired
  private PostRepository postRepository;
  @Autowired
  private UserRepository userRepository;

  @Transactional
  public Comment createComment(Long postId, Comment comment) {
    Post post =
            postRepository
                    .findById(postId.intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

    if (comment.getAuthor() == null || comment.getAuthor().getId() == null) {
      throw new IllegalArgumentException("Comment author is required");
    }

    User author =
            userRepository
                    .findById(comment.getAuthor().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

    comment.setAuthor(author);
    comment.setPost(post);

    Comment savedComment = commentRepository.save(comment);
    logger.info("Comment {} created by user {} on post {}", savedComment.getId(), author.getId(), postId);
    return savedComment;
  }

  public Comment addComment(Long postId, Long userId, String text) {
    Post post =
            postRepository
                    .findById(postId.intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Comment comment = new Comment(text, user, post);
    return commentRepository.save(comment);
  }

  public List<Comment> getComments(Long postId) {
    Post post =
            postRepository
                    .findById(postId.intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    return post.getComments();
  }

  @Transactional
  public void deleteComment(Long commentId, Long userId) {
    Comment comment = commentRepository
            .findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    
    // Проверяем, что пользователь является автором комментария
    if (comment.getAuthor() == null || !comment.getAuthor().getId().equals(userId)) {
      throw new IllegalArgumentException("Only the comment author can delete the comment");
    }
    
    // Удаляем все ответы на комментарий
    if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
      // Очищаем связи ManyToMany в ответах перед удалением
      for (Comment reply : comment.getReplies()) {
        if (reply.getLikedUsers() != null) {
          reply.getLikedUsers().clear();
        }
        if (reply.getDislikedUsers() != null) {
          reply.getDislikedUsers().clear();
        }
        commentRepository.save(reply);
      }
      commentRepository.deleteAll(comment.getReplies());
      logger.debug("Deleted {} replies for comment {}", comment.getReplies().size(), commentId);
    }
    
    // Очищаем связи ManyToMany перед удалением комментария
    if (comment.getLikedUsers() != null) {
      comment.getLikedUsers().clear();
    }
    if (comment.getDislikedUsers() != null) {
      comment.getDislikedUsers().clear();
    }
    commentRepository.save(comment);
    
    // Удаляем комментарий
    commentRepository.delete(comment);
    logger.info("Comment {} deleted by user {}", commentId, userId);
  }

  @Transactional
  public void addOrRemoveLike(Long userId, Long commentId) {
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Comment comment =
            commentRepository
                    .findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

    if (comment.getLikedUsers().contains(user)) {
      comment.getLikedUsers().remove(user);
    } else {
      comment.getDislikedUsers().remove(user);
      comment.getLikedUsers().add(user);
    }
    commentRepository.save(comment);
  }

  @Transactional
  public void addOrRemoveDislike(Long userId, Long commentId) {
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Comment comment =
            commentRepository
                    .findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

    if (comment.getDislikedUsers().contains(user)) {
      comment.getDislikedUsers().remove(user);
    } else {
      comment.getLikedUsers().remove(user);
      comment.getDislikedUsers().add(user);
    }
    commentRepository.save(comment);
  }
}
