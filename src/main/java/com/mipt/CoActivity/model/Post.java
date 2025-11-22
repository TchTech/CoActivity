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
@Table(name = "posts")
public class Post {
  @ManyToOne
  @JoinColumn(name = "userId")
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers"})
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
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers"})
  private List<User> likedUsers;
  @ManyToMany
  @JoinTable(
          name = "dislikesOnPosts",
          joinColumns = @JoinColumn(name = "postId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers"})
  private List<User> dislikedUsers;
  @OneToMany(mappedBy = "post")
  @JsonIgnoreProperties({"post", "author", "likedUsers", "dislikedUsers"})
  private List<Comment> comments;
  @ManyToOne
  @JoinColumn(name = "roomId")
  @JsonIgnoreProperties({"collaborators", "admins", "createdBy"})
  private Room room;
  @ManyToOne
  @JoinColumn(name = "imageId")
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
