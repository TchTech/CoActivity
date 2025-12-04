package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
@Data
@Entity
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @ManyToOne
  @JoinColumn(name = "userId")
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private User user;
  private String title;
  private String content;
  @Column(name = "type")
  private String type; // "MEMBERSHIP_REQUEST", "MEMBERSHIP_APPROVED", "MEMBERSHIP_REJECTED", "POST_PINNED", etc.
  @Column(name = "data", columnDefinition = "TEXT")
  private String data; // JSON string for contextual payload
  @Column(name = "isRead")
  private boolean isRead;
  @Column(name = "createdAt")
  private Instant createdAt;

  public void setIsRead(boolean b) {
    this.isRead = b;
  }
}
