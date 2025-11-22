package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {
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
}
