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

@Service
public class PostService {
  private static final Logger logger = LoggerFactory.getLogger(PostService.class);

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final RoomRepository roomRepository;
  private final ImageRepository imageRepository;
  private final RoomPostPinRepository roomPostPinRepository;
  private final CommentRepository commentRepository;

  @Autowired
  public PostService(
          PostRepository postRepository,
          UserRepository userRepository,
          RoomRepository roomRepository,
          ImageRepository imageRepository,
          RoomPostPinRepository roomPostPinRepository,
          CommentRepository commentRepository) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.imageRepository = imageRepository;
    this.roomPostPinRepository = roomPostPinRepository;
    this.commentRepository = commentRepository;
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

    if (post.getLikedUsers().contains(user)) {
      post.getLikedUsers().remove(user);
    } else {
      post.getDislikedUsers().remove(user);
      post.getLikedUsers().add(user);
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

    if (post.getDislikedUsers().contains(user)) {
      post.getDislikedUsers().remove(user);
    } else {
      post.getLikedUsers().remove(user);
      post.getDislikedUsers().add(user);
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
}
