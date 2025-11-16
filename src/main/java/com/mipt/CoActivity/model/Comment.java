package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@Entity
public class Comment {
  @ManyToOne
  @JoinColumn(name = "authorId")
  private User author;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String text;
  @ManyToMany
  @JoinTable(
          name = "likesOnComments",
          joinColumns = @JoinColumn(name = "commentId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
  private List<User> likedUsers;
  @ManyToMany
  @JoinTable(
          name = "dislikesOnComments",
          joinColumns = @JoinColumn(name = "commentId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
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
