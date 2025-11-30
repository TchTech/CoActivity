package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
  @Column(name = "name")
  private String name;

  @Column(name = "userName")
  private String username;

  @Column(name = "eMail")
  private String email;

  @Column(name = "phone")
  private String phone;

  @Column(name = "address")
  private String address;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "passwordHash")
  @JsonIgnore
  private String passwordHash;

  @ManyToMany(mappedBy = "collaborators")
  @JsonIgnore
  private List<Room> rooms = new ArrayList<>();

  @OneToMany(mappedBy = "author")
  @JsonIgnore
  private List<Post> posts = new ArrayList<>();

  @ManyToMany
  @JoinTable(
      name = "user_interests",
      joinColumns = @JoinColumn(name = "userId"),
      inverseJoinColumns = @JoinColumn(name = "interestId"))
  @JsonIgnoreProperties({"users"})
  private List<Interest> interests = new ArrayList<>();
  
  @OneToMany(mappedBy = "reviewedUser")
  @JsonIgnore
  private List<Feedback> feedbacks = new ArrayList<>();
  
  @OneToMany(mappedBy = "author")
  @JsonIgnore
  private List<Feedback> feedbacksAuthor = new ArrayList<>();

  @ManyToMany
  @JoinTable(
      name = "user_subscriptions",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "subscribed_to_user_id"))
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private List<User> subscriptions = new ArrayList<>();

  @ManyToMany(mappedBy = "subscriptions")
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private List<User> followers = new ArrayList<>();

  @Column(name = "createdAt")
  private Instant createdAt;

  @ManyToOne
  @JoinColumn(name = "avatarImageId")
  @JsonIgnoreProperties({"content"})
  private Image avatar;

  public User(String username, String email, String passwordHash) {
    this.username = username;
    this.name = username;
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
