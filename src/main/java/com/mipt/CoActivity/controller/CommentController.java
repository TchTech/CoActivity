package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Comment;
import com.mipt.CoActivity.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {
  @Autowired private CommentService commentService;

  @PostMapping("/add")
  public Comment addComment(
      @RequestParam Long postId, @RequestParam Long userId, @RequestParam String text) {
    return commentService.addComment(postId, userId, text);
  }

  @DeleteMapping("/delete")
  public void deleteComment(@RequestParam Long commentId) {
    commentService.deleteComment(commentId);
  }

  @GetMapping("/{postId}")
  public List<Comment> getCommentsUnderPost(@PathVariable Long postId) {
    return commentService.getComments(postId);
  }
}
