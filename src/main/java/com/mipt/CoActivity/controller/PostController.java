package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.PostRepository;
import com.mipt.CoActivity.repository.UserRepository;
import com.mipt.CoActivity.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {
  @Autowired private PostRepository postRepository;
  @Autowired private PostService postService;

  @Autowired private UserRepository userRepository;

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
