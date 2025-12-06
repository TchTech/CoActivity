package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.dto.RecommendedPostsResponse;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class PostController {
  private static final Logger logger = LoggerFactory.getLogger(PostController.class);
  @Autowired private PostService postService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Post> createPost(@RequestBody Post post) {
    return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(post));
  }

  @PostMapping("/publish")
  public Post publishPost(
      @RequestParam String name,
      @RequestParam User author,
      @RequestParam String text,
      @RequestParam Image image) {
    return postService.publishPost(name, author, text, image);
  }

  @PostMapping("/{postId}/like")
  public void like(@RequestParam Long userId, @PathVariable Long postId) {
    postService.addOrRemoveLike(userId, postId);
  }

  @PostMapping("/{postId}/dislike")
  public void dislike(@RequestParam Long userId, @PathVariable Long postId) {
    postService.addOrRemoveDislike(userId, postId);
  }

  @GetMapping
  public ResponseEntity<List<Post>> getAllPosts() {
    return ResponseEntity.ok(postService.getAllPosts());
  }

  @GetMapping("/{postId}")
  public ResponseEntity<Post> getPostById(@PathVariable Long postId) {
    return ResponseEntity.ok(postService.getPostById(postId));
  }

  @DeleteMapping("/{postId}")
  public ResponseEntity<?> deletePost(@PathVariable Long postId, @RequestParam Long userId) {
    try {
      postService.deletePost(postId, userId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      logger.warn("Delete post forbidden: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", e.getMessage()));
    } catch (ResourceNotFoundException e) {
      logger.warn("Post not found: {}", postId);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(java.util.Map.of("error", "Post not found"));
    } catch (Exception e) {
      logger.error("Error deleting post {}: {}", postId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(java.util.Map.of("error", "Failed to delete post: " + e.getMessage()));
    }
  }

  @GetMapping("/recommended")
  public ResponseEntity<?> getRecommendedPosts(@RequestParam Long userId, 
                                                @RequestParam(required = false, defaultValue = "false") Boolean includeScores) {
    try {
      if (Boolean.TRUE.equals(includeScores)) {
        // Возвращаем ответ с scores
        com.mipt.CoActivity.dto.RecommendedPostsResponse response = postService.getRecommendedPostsWithScores(userId);
        logger.info("Returning {} recommended posts with scores for user {}", response.getPosts().size(), userId);
        return ResponseEntity.ok(response);
      } else {
        // Возвращаем только посты (обратная совместимость)
        List<Post> recommendedPosts = postService.getRecommendedPosts(userId);
        logger.info("Returning {} recommended posts for user {}", recommendedPosts.size(), userId);
        return ResponseEntity.ok(recommendedPosts);
      }
    } catch (ResourceNotFoundException e) {
      logger.warn("User not found: {}", userId);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new java.util.ArrayList<>());
    } catch (Exception e) {
      logger.error("Error getting recommended posts for user {}: {}", userId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new java.util.ArrayList<>());
    }
  }
}
