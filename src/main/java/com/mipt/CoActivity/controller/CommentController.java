package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Comment;
import com.mipt.CoActivity.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts/{postId}/comments")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class CommentController {

  @Autowired private CommentService commentService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Comment> createComment(
      @PathVariable Long postId, @RequestBody Comment comment) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(commentService.createComment(postId, comment));
  }

  @PostMapping("/{parentCommentId}/reply")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Comment> createReply(
      @PathVariable Long postId, 
      @PathVariable Long parentCommentId,
      @RequestBody Comment comment) {
    // Устанавливаем родительский комментарий
    Comment parentComment = new Comment();
    parentComment.setId(parentCommentId);
    comment.setParentComment(parentComment);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(commentService.createComment(postId, comment));
  }

  @GetMapping
  public ResponseEntity<List<Comment>> getComments(@PathVariable Long postId) {
    return ResponseEntity.ok(commentService.getComments(postId));
  }

  @PostMapping("/{commentId}/like")
  public void likeComment(
      @PathVariable Long postId, @PathVariable Long commentId, @RequestParam Long userId) {
    commentService.addOrRemoveLike(userId, commentId);
  }

  @PostMapping("/{commentId}/dislike")
  public void dislikeComment(
      @PathVariable Long postId, @PathVariable Long commentId, @RequestParam Long userId) {
    commentService.addOrRemoveDislike(userId, commentId);
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<?> deleteComment(
      @PathVariable Long postId, 
      @PathVariable Long commentId,
      @RequestParam Long userId) {
    try {
      commentService.deleteComment(commentId, userId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", e.getMessage()));
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(java.util.Map.of("error", "Comment not found"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(java.util.Map.of("error", "Failed to delete comment: " + e.getMessage()));
    }
  }
}
