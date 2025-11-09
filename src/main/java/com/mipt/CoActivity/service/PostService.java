package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.PostRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;

  @Autowired
  public PostService(PostRepository postRepository, UserRepository userRepository) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
  }

  public void addOrRemoveLike(Long userId, Long postId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    Post post =
        postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
    if (post.getLikedUsers().contains(user)) {
      post.getLikedUsers().remove(user);
    } else {
      post.getLikedUsers().add(user);
    }
  }
  public void addOrRemoveDislike(Long userId, Long postId) {
    User user =
            userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    Post post =
            postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
    if (post.getDislikedUsers().contains(user)) {
      post.getDislikedUsers().remove(user);
    } else {
      post.getDislikedUsers().add(user);
    }
  }

  public Post publishPost(String name, User author, String text, Image image) {
    Post post = new Post(name, author, text, image);
    return postRepository.save(post);
  }

  public List<Post> getAllPosts() {
    return postRepository.findAll();
  }
}
