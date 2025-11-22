package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Comment;
import com.mipt.CoActivity.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {

  @Autowired private CommentService commentService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Comment> createComment(
      @PathVariable Long postId, @RequestBody Comment comment) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(commentService.createComment(postId, comment));
  }
}
