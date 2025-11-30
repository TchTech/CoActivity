package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
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
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private List<User> likedUsers = new ArrayList<>();
  @ManyToMany
  @JoinTable(
          name = "dislikesOnComments",
          joinColumns = @JoinColumn(name = "commentId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private List<User> dislikedUsers = new ArrayList<>();
  @ManyToOne
  @JoinColumn(name="postId")
  @JsonIgnoreProperties({"comments", "author", "likedUsers", "dislikedUsers", "room"})
  private Post post;
  public Comment(String text, User user, Post post) {
    this.author = user;
    this.text = text;
    this.post = post;
    this.likedUsers = new ArrayList<>();
    this.dislikedUsers = new ArrayList<>();
  }
}
