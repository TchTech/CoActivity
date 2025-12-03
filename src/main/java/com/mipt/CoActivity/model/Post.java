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
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
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
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private List<User> likedUsers = new ArrayList<>();
  @ManyToMany
  @JoinTable(
          name = "dislikesOnPosts",
          joinColumns = @JoinColumn(name = "postId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private List<User> dislikedUsers = new ArrayList<>();
  @OneToMany(mappedBy = "post")
  @JsonIgnoreProperties({"post", "author", "likedUsers", "dislikedUsers"})
  private List<Comment> comments = new ArrayList<>();
  @ManyToOne
  @JoinColumn(name = "roomId")
  @JsonIgnoreProperties({"collaborators", "admins", "createdBy"})
  private Room room;
  @ManyToOne
  @JoinColumn(name = "imageId")
  private Image image;
  
  @Column(name = "externalLinks", length = 1000)
  private String externalLinks; // JSON array of URLs or comma-separated URLs
  
  @OneToMany(mappedBy = "post")
  @JsonIgnoreProperties({"post", "room", "pinnedBy"})
  private List<RoomPostPin> pinnedToRooms = new ArrayList<>();
  
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
