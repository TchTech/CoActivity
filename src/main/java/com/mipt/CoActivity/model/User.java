package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "users")
public class User {
  @Column(name = "userName")
  private String username;

  @Column(name = "eMail")
  private String email;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "passwordHash")
  private String passwordHash;

  @ManyToMany(mappedBy = "collaborators")
  private List<Room> rooms;

  @OneToMany(mappedBy = "author")
  private List<Post> posts;

  @ManyToMany
  @JoinTable(
      name = "user_interests",
      joinColumns = @JoinColumn(name = "userId"),
      inverseJoinColumns = @JoinColumn(name = "interestId"))
  private List<Interest> interests;
  
  @OneToMany(mappedBy = "reviewedUser")
  private List<Feedback> feedbacks;
  
  @OneToMany(mappedBy = "author")
  private List<Feedback> feedbacksAuthor;

  @ManyToMany
  @JoinTable(
      name = "user_subscriptions",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "subscribed_to_user_id"))
  private List<User> subscriptions;

  @ManyToMany(mappedBy = "subscriptions")
  private List<User> followers;

  @Column(name = "createdAt")
  private Instant createdAt;

  public User(String username, String email, String passwordHash) {
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.rooms = new ArrayList<>();
    this.feedbacks = new ArrayList<>();
    this.feedbacksAuthor = new ArrayList<>();
    this.createdAt = Instant.now();
    this.subscriptions = new ArrayList<>();
    this.followers = new ArrayList<>();
  }
}
