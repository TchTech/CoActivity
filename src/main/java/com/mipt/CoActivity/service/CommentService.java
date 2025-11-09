package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Comment;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.CommentRepository;
import com.mipt.CoActivity.repository.PostRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

  @Autowired private CommentRepository commentRepository;

  @Autowired private PostRepository postRepository;

  @Autowired private UserRepository userRepository;

  public Comment addComment(Long postId, Long userId, String text) {
    Post post =
        postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    Comment comment = new Comment(text, user, post);
    return commentRepository.save(comment);
  }

  public List<Comment> getComments(Long postId) {
    Post post =
        postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
    return post.getComments();
  }

  public void deleteComment(Long commentId) {
    commentRepository.deleteById(commentId);
  }
}
