package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
@Entity
public class Comment {
  @ManyToOne
  @JoinColumn(name = "authorId")
  private User author;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String text;
  private List<User> likedUsers;
  private List<User> dislikedUsers;
  @ManyToOne
  @JoinColumn(name="postId")
  private Post post;
  public Comment(String text, User user, Post post) {
    this.author = user;
    this.text = text;
    this.post = post;
    this.likedUsers = new ArrayList<>();
    this.dislikedUsers = new ArrayList<>();
  }
}
