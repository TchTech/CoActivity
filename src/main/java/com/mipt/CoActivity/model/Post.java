package com.mipt.CoActivity.model;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Data
public class Post {
  @ManyToOne
  @JoinColumn(name = "userId")
  private User author;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private String name;
  private String text;
  @ManyToMany
  @JoinTable(
          name = "likesOnPosts",
          joinColumns = @JoinColumn(name = "postId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
  private List<User> likedUsers;
  @ManyToMany
  @JoinTable(
          name = "dislikesOnPosts",
          joinColumns = @JoinColumn(name = "postId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
  private List<User> dislikedUsers;
  @OneToMany(mappedBy = "post")
  private List<Comment> comments;
  private Room room;
  private Image image;
  public Post(String name, User author, String text, Image image) {
    this.name = name;
    this.author = author;
    this.text = text;
    this.image = image;
    this.comments = new ArrayList<>();
    this.likedUsers = new ArrayList<>();
    this.dislikedUsers = new ArrayList<>();
  }
}
