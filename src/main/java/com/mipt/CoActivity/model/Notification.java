package com.mipt.CoActivity.model;

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
  private User user;
  private String title;
  private String content;
  @Column(name = "isRead")
  private boolean isRead;
  @Column(name = "createdAt")
  private Instant createdAt;
}
