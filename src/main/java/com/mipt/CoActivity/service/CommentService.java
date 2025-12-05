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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashSet;
import java.util.Set;

@Service
public class CommentService {
  private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

  @Autowired
  private CommentRepository commentRepository;
  @Autowired
  private PostRepository postRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private NotificationService notificationService;

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
    
    // Send notification to post author about new comment (only if not commented by author themselves)
    if (post.getAuthor() != null && !post.getAuthor().getId().equals(author.getId())) {
      try {
        String commenterName = author.getName() != null ? author.getName() : author.getUsername();
        String postTitle = post.getName() != null ? post.getName() : "пост";
        String content = String.format("%s прокомментировал(а) ваш пост \"%s\"", commenterName, postTitle);
        
        notificationService.createNotification(
            post.getAuthor().getId(),
            "POST_COMMENTED",
            "Новый комментарий",
            content,
            String.format("{\"postId\":%d,\"commentId\":%d,\"commenterId\":%d,\"commenterName\":\"%s\"}", 
                postId.intValue(), savedComment.getId().intValue(), author.getId(), commenterName)
        );
        logger.info("Sent comment notification to post author {} for post {}", post.getAuthor().getId(), postId);
      } catch (Exception e) {
        logger.error("Failed to send comment notification: {}", e.getMessage(), e);
        // Don't fail comment creation if notification fails
      }
    }
    
    // Send notification to parent comment author if this is a reply
    if (savedComment.getParentComment() != null && savedComment.getParentComment().getAuthor() != null) {
      User parentAuthor = savedComment.getParentComment().getAuthor();
      if (!parentAuthor.getId().equals(author.getId())) {
        try {
          String commenterName = author.getName() != null ? author.getName() : author.getUsername();
          String content = String.format("%s ответил(а) на ваш комментарий", commenterName);
          
          notificationService.createNotification(
              parentAuthor.getId(),
              "COMMENT_REPLY",
              "Ответ на комментарий",
              content,
              String.format("{\"postId\":%d,\"commentId\":%d,\"parentCommentId\":%d,\"commenterId\":%d,\"commenterName\":\"%s\"}", 
                  postId.intValue(), savedComment.getId().intValue(), savedComment.getParentComment().getId().intValue(), author.getId(), commenterName)
          );
          logger.info("Sent reply notification to comment author {} for comment {}", parentAuthor.getId(), savedComment.getParentComment().getId());
        } catch (Exception e) {
          logger.error("Failed to send reply notification: {}", e.getMessage(), e);
          // Don't fail comment creation if notification fails
        }
      }
    }
    
    // Send notifications for mentions in comment text
    try {
      sendMentionNotifications(savedComment.getText(), author.getId(), savedComment.getId().longValue(), "COMMENT");
    } catch (Exception e) {
      logger.error("Failed to send mention notifications: {}", e.getMessage(), e);
      // Don't fail comment creation if notification fails
    }
    
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

  /**
   * Extract mentions from text and send notifications to mentioned users.
   * Mentions are in format @username
   */
  private void sendMentionNotifications(String text, Long authorId, Long entityId, String entityType) {
    if (text == null || text.trim().isEmpty()) {
      return;
    }

    // Pattern to match @username mentions
    Pattern mentionPattern = Pattern.compile("@(\\w+)");
    Matcher matcher = mentionPattern.matcher(text);
    Set<String> mentionedUsernames = new HashSet<>();

    while (matcher.find()) {
      String username = matcher.group(1);
      mentionedUsernames.add(username.toLowerCase());
    }

    if (mentionedUsernames.isEmpty()) {
      return;
    }

    // Find users by username
    User author = userRepository.findById(authorId)
        .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
    String authorName = author.getName() != null ? author.getName() : author.getUsername();

    for (String username : mentionedUsernames) {
      try {
        User mentionedUser = userRepository.findByUsername(username);
        if (mentionedUser == null) {
          // Try to find by name (case-insensitive)
          List<User> usersByName = userRepository.findAll().stream()
              .filter(u -> u.getName() != null && u.getName().toLowerCase().equals(username))
              .toList();
          if (usersByName.isEmpty()) {
            logger.debug("User with username/name '{}' not found, skipping mention notification", username);
            continue;
          }
          mentionedUser = usersByName.get(0);
        }

        // Don't notify if user mentioned themselves
        if (mentionedUser.getId().equals(authorId)) {
          continue;
        }

        String content = String.format("%s упомянул(а) вас в %s", authorName, 
            "POST".equals(entityType) ? "посте" : "комментарии");
        
        notificationService.createNotification(
            mentionedUser.getId(),
            "MENTION",
            "Вас упомянули",
            content,
            String.format("{\"%sId\":%d,\"authorId\":%d,\"authorName\":\"%s\"}", 
                entityType.toLowerCase(), entityId, authorId, authorName)
        );
        logger.debug("Sent mention notification to user {} for {} {}", mentionedUser.getId(), entityType, entityId);
      } catch (Exception e) {
        logger.error("Failed to send mention notification for username '{}': {}", username, e.getMessage(), e);
        // Continue with other mentions
      }
    }
  }
}
