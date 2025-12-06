package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.PostRecommendationRequest;
import com.mipt.CoActivity.dto.PostRecommendationResponse;
import com.mipt.CoActivity.dto.RecommendedPostsResponse;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Interest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
  private final PostRecommendationService postRecommendationService;

  @Autowired
  public PostService(
          PostRepository postRepository,
          UserRepository userRepository,
          RoomRepository roomRepository,
          ImageRepository imageRepository,
          RoomPostPinRepository roomPostPinRepository,
          CommentRepository commentRepository,
          NotificationService notificationService,
          PostRecommendationService postRecommendationService) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.imageRepository = imageRepository;
    this.roomPostPinRepository = roomPostPinRepository;
    this.commentRepository = commentRepository;
    this.notificationService = notificationService;
    this.postRecommendationService = postRecommendationService;
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

  /**
   * Получает рекомендации постов для пользователя (без scores, обратная совместимость)
   */
  public List<Post> getRecommendedPosts(Long userId) {
    RecommendedPostsResponse response = getRecommendedPostsWithScores(userId);
    return response.getPosts() != null ? response.getPosts() : new ArrayList<>();
  }

  /**
   * Получает рекомендации постов для пользователя с similarity scores
   */
  public RecommendedPostsResponse getRecommendedPostsWithScores(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    List<Post> candidatePosts;
    List<String> userInterests = new ArrayList<>();
    List<Integer> subscribedUserIds = new ArrayList<>();
    
    // Проверяем, есть ли у пользователя подписки
    List<User> subscriptions = user.getSubscriptions();
    if (subscriptions != null && !subscriptions.isEmpty()) {
      // Если есть подписки - используем все посты, но приоритизируем посты от подписок
      logger.info("User {} has {} subscriptions, prioritizing subscription posts in recommendations", 
              userId, subscriptions.size());
      
      List<Long> subscribedIds = subscriptions.stream()
              .map(User::getId)
              .collect(Collectors.toList());
      
      // Используем ВСЕ посты, но приоритизируем посты от подписок через Python API
      candidatePosts = getAllPosts();
      subscribedUserIds = subscribedIds.stream()
              .map(Long::intValue)
              .collect(Collectors.toList());
      
      // Также собираем интересы пользователя для улучшения рекомендаций
      if (user.getInterests() != null) {
        userInterests = user.getInterests().stream()
                .map(Interest::getName)
                .collect(Collectors.toList());
      }
    } else {
      // Если подписок нет - используем все посты и интересы пользователя
      logger.info("User {} has no subscriptions, using interests for recommendations", userId);
      
      candidatePosts = getAllPosts();
      
      // Получаем интересы пользователя
      if (user.getInterests() != null && !user.getInterests().isEmpty()) {
        userInterests = user.getInterests().stream()
                .map(Interest::getName)
                .collect(Collectors.toList());
      }
      
      // Если нет интересов, возвращаем все посты без рекомендаций
      if (userInterests.isEmpty()) {
        logger.warn("User {} has no interests, returning all posts without recommendations", userId);
        List<Post> sortedPosts = candidatePosts.stream()
                .sorted((a, b) -> {
                  // Сортируем по дате создания (новые первыми)
                  if (a.getId() != null && b.getId() != null) {
                    return b.getId().compareTo(a.getId());
                  }
                  return 0;
                })
                .collect(Collectors.toList());
        // Возвращаем с нулевыми scores
        List<Double> defaultScores = sortedPosts.stream().map(p -> 1.0).collect(Collectors.toList());
        return new RecommendedPostsResponse(sortedPosts, defaultScores, "No interests, returning all posts");
      }
    }
    
    if (candidatePosts.isEmpty()) {
      logger.info("No candidate posts found for user {}", userId);
      return new RecommendedPostsResponse(new ArrayList<>(), new ArrayList<>(), "No posts available");
    }
    
    // Преобразуем посты в формат для Python API
    List<PostRecommendationRequest.PostData> postDataList = candidatePosts.stream()
            .map(post -> {
              PostRecommendationRequest.PostData postData = new PostRecommendationRequest.PostData();
              postData.setId(post.getId());
              postData.setName(post.getName() != null ? post.getName() : "");
              postData.setText(post.getText() != null ? post.getText() : "");
              
              // Информация об авторе
              if (post.getAuthor() != null) {
                postData.setAuthorId(post.getAuthor().getId() != null ? 
                        post.getAuthor().getId().intValue() : null);
                postData.setAuthorName(post.getAuthor().getName() != null ? 
                        post.getAuthor().getName() : post.getAuthor().getUsername());
                
                // Собираем интересы автора
                if (post.getAuthor().getInterests() != null) {
                  List<String> authorInterests = post.getAuthor().getInterests().stream()
                          .map(Interest::getName)
                          .collect(Collectors.toList());
                  postData.setAuthorInterests(authorInterests);
                }
              }
              
              // Информация о комнате
              if (post.getRoom() != null) {
                postData.setRoomName(post.getRoom().getName());
                postData.setRoomCategory(post.getRoom().getCategory());
              }
              
              return postData;
            })
            .collect(Collectors.toList());
    
    // Формируем запрос к Python API
    PostRecommendationRequest request = new PostRecommendationRequest();
    request.setUserInterests(userInterests);
    request.setPosts(postDataList);
    request.setSubscribedUserIds(subscribedUserIds);
    
    // Получаем рекомендации от Python API
    PostRecommendationResponse response = postRecommendationService.getRecommendations(request);
    
    if (response == null || response.getRecommendedPosts() == null || response.getRecommendedPosts().isEmpty()) {
      logger.warn("No recommendations received from Python API for user {}, returning all candidate posts", userId);
      // Возвращаем все посты, отсортированные по дате
      List<Post> sortedPosts = candidatePosts.stream()
              .sorted((a, b) -> {
                if (a.getId() != null && b.getId() != null) {
                  return b.getId().compareTo(a.getId());
                }
                return 0;
              })
              .collect(Collectors.toList());
      // Возвращаем с нулевыми scores
      List<Double> defaultScores = sortedPosts.stream().map(p -> 1.0).collect(Collectors.toList());
      return new RecommendedPostsResponse(sortedPosts, defaultScores, "No recommendations from Python API, returning all posts");
    }
    
    // Создаем Map для быстрого поиска постов по ID
    Map<Integer, Post> postMap = candidatePosts.stream()
            .collect(Collectors.toMap(Post::getId, post -> post));
    
    // Преобразуем рекомендации обратно в Post объекты
    List<Post> recommendedPosts = response.getRecommendedPosts().stream()
            .map(postData -> postMap.get(postData.getId()))
            .filter(post -> post != null)
            .collect(Collectors.toList());
    
    logger.info("Received {} recommended posts from Python API for user {}", 
            recommendedPosts.size(), userId);
    
    // Логируем scores в консоль Java
    if (response.getSimilarityScores() != null && !response.getSimilarityScores().isEmpty()) {
      logger.info("Similarity scores for user {}: {}", userId, response.getSimilarityScores());
      for (int i = 0; i < recommendedPosts.size() && i < response.getSimilarityScores().size(); i++) {
        logger.info("Post {} (ID: {}) - Similarity Score: {}", 
            i + 1, 
            recommendedPosts.get(i).getId(), 
            response.getSimilarityScores().get(i));
      }
    }
    
    RecommendedPostsResponse result = new RecommendedPostsResponse(
        recommendedPosts,
        response.getSimilarityScores() != null ? response.getSimilarityScores() : new ArrayList<>(),
        response.getMessage() != null ? response.getMessage() : "Recommendations received"
    );
    
    return result;
  }
}
