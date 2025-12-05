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
    
    // Если это ответ на комментарий, устанавливаем родительский комментарий
    if (comment.getParentComment() != null && comment.getParentComment().getId() != null) {
      Comment parentComment = commentRepository
              .findById(comment.getParentComment().getId())
              .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
      comment.setParentComment(parentComment);
    }

    Comment savedComment = commentRepository.save(comment);
    logger.info("Comment {} created by user {} on post {} (parent: {})", 
        savedComment.getId(), author.getId(), postId, 
        savedComment.getParentComment() != null ? savedComment.getParentComment().getId() : "none");
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
    // Возвращаем только корневые комментарии (без родительского комментария)
    List<Comment> rootComments = commentRepository.findByPostIdAndParentCommentIsNull(postId.intValue());
    
    // Загружаем ответы для каждого корневого комментария
    for (Comment comment : rootComments) {
      List<Comment> replies = commentRepository.findByParentCommentId(comment.getId());
      comment.setReplies(replies);
    }
    
    return rootComments;
  }

  @Transactional
  public void deleteComment(Long commentId, Long userId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    
    // Проверяем, что пользователь является автором комментария
    if (comment.getAuthor() == null || !comment.getAuthor().getId().equals(userId)) {
      throw new IllegalArgumentException("Only the comment author can delete the comment");
    }
    
    // Удаляем дочерние комментарии (replies) сначала
    if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
      for (Comment reply : comment.getReplies()) {
        // Очищаем связи ManyToMany для каждого ответа
        if (reply.getLikedUsers() != null) {
          reply.getLikedUsers().clear();
        }
        if (reply.getDislikedUsers() != null) {
          reply.getDislikedUsers().clear();
        }
        commentRepository.save(reply);
      }
      // Удаляем дочерние комментарии
      commentRepository.deleteAll(comment.getReplies());
    }
    
    // Очищаем связи ManyToMany перед удалением
    if (comment.getLikedUsers() != null) {
      comment.getLikedUsers().clear();
    }
    if (comment.getDislikedUsers() != null) {
      comment.getDislikedUsers().clear();
    }
    
    // Сохраняем изменения для очистки связей
    commentRepository.save(comment);
    
    // Удаляем комментарий
    commentRepository.delete(comment);
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
