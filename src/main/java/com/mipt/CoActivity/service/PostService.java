package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.ImageRepository;
import com.mipt.CoActivity.repository.PostRepository;
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

  @Autowired
  public PostService(
          PostRepository postRepository,
          UserRepository userRepository,
          RoomRepository roomRepository,
          ImageRepository imageRepository) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.imageRepository = imageRepository;
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
                    .findById(postId)
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
                    .findById(postId)
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
    return postRepository.findAll();
  }
}
