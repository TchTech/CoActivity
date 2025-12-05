package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.RoomPostPin;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.CommentRepository;
import com.mipt.CoActivity.repository.ImageRepository;
import com.mipt.CoActivity.repository.PostRepository;
import com.mipt.CoActivity.repository.RoomPostPinRepository;
import com.mipt.CoActivity.repository.RoomRepository;
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
public class PostService {
  private static final Logger logger = LoggerFactory.getLogger(PostService.class);

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final RoomRepository roomRepository;
  private final ImageRepository imageRepository;
  private final RoomPostPinRepository roomPostPinRepository;
  private final CommentRepository commentRepository;
  private final NotificationService notificationService;

  @Autowired
  public PostService(
          PostRepository postRepository,
          UserRepository userRepository,
          RoomRepository roomRepository,
          ImageRepository imageRepository,
          RoomPostPinRepository roomPostPinRepository,
          CommentRepository commentRepository,
          NotificationService notificationService) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.imageRepository = imageRepository;
    this.roomPostPinRepository = roomPostPinRepository;
    this.commentRepository = commentRepository;
    this.notificationService = notificationService;
  }

  @Transactional
  public Post createPost(Post post) {
    if (post.getAuthor() == null || post.getAuthor().getId() == null) {
      throw new IllegalArgumentException("Post author is required");
    }

    User author =
            userRepository
                    .findById(post.getAuthor().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

    post.setAuthor(author);

    if (post.getRoom() != null && post.getRoom().getId() != null) {
      Room room =
              roomRepository
                      .findById(post.getRoom().getId())
                      .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
      post.setRoom(room);
    }

    if (post.getImage() != null && post.getImage().getId() != null) {
      Image image =
              imageRepository
                      .findById(post.getImage().getId())
                      .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
      post.setImage(image);
    }

    Post savedPost = postRepository.save(post);
    logger.info("Post {} created by user {}", savedPost.getId(), author.getId());
    
    // If room is specified, automatically pin the post to that room
    if (post.getRoom() != null && post.getRoom().getId() != null) {
      Room room = roomRepository.findById(post.getRoom().getId())
          .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
      
      // Check if user is a member of the room
      if (!room.getCollaborators().contains(author)) {
        logger.warn("User {} is not a member of room {}, cannot pin post", author.getId(), room.getId());
      } else {
        // Check if already pinned
        java.util.Optional<RoomPostPin> existingPin = roomPostPinRepository.findByRoomIdAndPostId(room.getId(), savedPost.getId());
        if (!existingPin.isPresent()) {
          RoomPostPin pin = new RoomPostPin(room, savedPost, author);
          roomPostPinRepository.save(pin);
          logger.info("Post {} automatically pinned to room {} by author {}", savedPost.getId(), room.getId(), author.getId());
        }
      }
    }
    
    // Send notifications to followers about new post
    try {
      sendNewPostNotifications(savedPost, author);
    } catch (Exception e) {
      logger.error("Failed to send new post notifications: {}", e.getMessage(), e);
      // Don't fail post creation if notification fails
    }
    
    // Send notifications for mentions in post text
    try {
      sendMentionNotifications(savedPost.getText(), author.getId(), savedPost.getId().longValue(), "POST");
    } catch (Exception e) {
      logger.error("Failed to send mention notifications: {}", e.getMessage(), e);
      // Don't fail post creation if notification fails
    }
    
    return savedPost;
  }

  @Transactional
  public void addOrRemoveLike(Long userId, Long postId) {
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Post post =
            postRepository
                    .findById(postId.intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

    boolean wasLiked = post.getLikedUsers().contains(user);
    
    if (wasLiked) {
      post.getLikedUsers().remove(user);
    } else {
      post.getDislikedUsers().remove(user);
      post.getLikedUsers().add(user);
      
      // Send notification to post author about like (only if not liked by author themselves)
      if (post.getAuthor() != null && !post.getAuthor().getId().equals(userId)) {
        try {
          String likerName = user.getName() != null ? user.getName() : user.getUsername();
          String postTitle = post.getName() != null ? post.getName() : "пост";
          String content = String.format("%s поставил(а) лайк вашему посту \"%s\"", likerName, postTitle);
          
          notificationService.createNotification(
              post.getAuthor().getId(),
              "POST_LIKED",
              "Новый лайк",
              content,
              String.format("{\"postId\":%d,\"likerId\":%d,\"likerName\":\"%s\"}", postId.intValue(), userId, likerName)
          );
          logger.info("Sent like notification to post author {} for post {}", post.getAuthor().getId(), postId);
        } catch (Exception e) {
          logger.error("Failed to send like notification: {}", e.getMessage(), e);
          // Don't fail like operation if notification fails
        }
      }
    }
    postRepository.save(post);
  }

  @Transactional
  public void addOrRemoveDislike(Long userId, Long postId) {
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Post post =
            postRepository
                    .findById(postId.intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

    boolean wasDisliked = post.getDislikedUsers().contains(user);
    
    if (wasDisliked) {
      post.getDislikedUsers().remove(user);
    } else {
      post.getLikedUsers().remove(user);
      post.getDislikedUsers().add(user);
      
      // Send notification to post author about dislike (only if not disliked by author themselves)
      if (post.getAuthor() != null && !post.getAuthor().getId().equals(userId)) {
        try {
          String dislikerName = user.getName() != null ? user.getName() : user.getUsername();
          String postTitle = post.getName() != null ? post.getName() : "пост";
          String content = String.format("%s поставил(а) дизлайк вашему посту \"%s\"", dislikerName, postTitle);
          
          notificationService.createNotification(
              post.getAuthor().getId(),
              "POST_DISLIKED",
              "Новый дизлайк",
              content,
              String.format("{\"postId\":%d,\"dislikerId\":%d,\"dislikerName\":\"%s\"}", postId.intValue(), userId, dislikerName)
          );
          logger.info("Sent dislike notification to post author {} for post {}", post.getAuthor().getId(), postId);
        } catch (Exception e) {
          logger.error("Failed to send dislike notification: {}", e.getMessage(), e);
          // Don't fail dislike operation if notification fails
        }
      }
    }
    postRepository.save(post);
  }

  public Post publishPost(String name, User author, String text, Image image) {
    Post post = new Post(name, author, text, image);
    return postRepository.save(post);
  }

  public List<Post> getAllPosts() {
    List<Post> posts = postRepository.findAll();
    // Load room and pinned rooms for each post
    for (Post post : posts) {
      // Trigger lazy loading for room
      if (post.getRoom() != null) {
        post.getRoom().getName(); // Trigger lazy loading
      }
      // Trigger lazy loading for pinned rooms
      if (post.getPinnedToRooms() != null) {
        post.getPinnedToRooms().size(); // Trigger lazy loading
      }
    }
    return posts;
  }

  public Post getPostById(Long postId) {
    return postRepository
            .findById(postId.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
  }

  @Transactional
  public void deletePost(Long postId, Long userId) {
    try {
      Post post = postRepository
              .findById(postId.intValue())
              .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
      
      // Проверяем, что пользователь является автором поста
      if (post.getAuthor() == null || !post.getAuthor().getId().equals(userId)) {
        throw new IllegalArgumentException("Only the post author can delete the post");
      }
      
      // Удаляем связанные записи RoomPostPin
      List<com.mipt.CoActivity.model.RoomPostPin> pins = roomPostPinRepository.findByPostId(postId.intValue());
      if (pins != null && !pins.isEmpty()) {
        roomPostPinRepository.deleteAll(pins);
        logger.debug("Deleted {} RoomPostPin records for post {}", pins.size(), postId);
      }
      
      // Получаем и удаляем все комментарии поста перед удалением поста
      // Сначала очищаем связи ManyToMany в комментариях
      List<com.mipt.CoActivity.model.Comment> comments = commentRepository.findByPostId(postId.intValue());
      if (comments != null && !comments.isEmpty()) {
        for (com.mipt.CoActivity.model.Comment comment : comments) {
          if (comment.getLikedUsers() != null) {
            comment.getLikedUsers().clear();
          }
          if (comment.getDislikedUsers() != null) {
            comment.getDislikedUsers().clear();
          }
          commentRepository.save(comment);
        }
        // Теперь удаляем комментарии
        commentRepository.deleteAll(comments);
        logger.debug("Deleted {} comments for post {}", comments.size(), postId);
      }
      
      // Очищаем связи ManyToMany перед удалением
      if (post.getLikedUsers() != null) {
        post.getLikedUsers().clear();
      }
      if (post.getDislikedUsers() != null) {
        post.getDislikedUsers().clear();
      }
      
      // Сохраняем изменения для очистки связей
      postRepository.save(post);
      
      // Удаляем пост
      postRepository.delete(post);
      logger.info("Post {} deleted by user {}", postId, userId);
    } catch (Exception e) {
      logger.error("Error deleting post {}: {}", postId, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Send notifications to all followers about a new post.
   */
  private void sendNewPostNotifications(Post post, User author) {
    if (author.getFollowers() == null || author.getFollowers().isEmpty()) {
      logger.debug("Author {} has no followers, skipping new post notifications", author.getId());
      return;
    }

    String postTitle = post.getName() != null ? post.getName() : "новый пост";
    String authorName = author.getName() != null ? author.getName() : author.getUsername();
    
    for (User follower : author.getFollowers()) {
      // Don't send notification to the author themselves
      if (follower.getId().equals(author.getId())) {
        continue;
      }
      
      try {
        String content = String.format("%s опубликовал(а) новый пост: \"%s\"", authorName, postTitle);
        
        notificationService.createNotification(
            follower.getId(),
            "NEW_POST",
            "Новый пост",
            content,
            String.format("{\"postId\":%d,\"authorId\":%d,\"authorName\":\"%s\"}", post.getId().intValue(), author.getId(), authorName)
        );
        logger.debug("Sent new post notification to follower {} for post {}", follower.getId(), post.getId());
      } catch (Exception e) {
        logger.error("Failed to send new post notification to follower {}: {}", follower.getId(), e.getMessage(), e);
        // Continue with other followers
      }
    }
    
    logger.info("Sent new post notifications to followers of user {}", author.getId());
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
