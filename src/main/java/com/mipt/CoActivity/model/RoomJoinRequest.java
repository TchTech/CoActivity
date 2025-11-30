package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "room_join_requests")
public class RoomJoinRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "roomId")
  @JsonIgnoreProperties({"collaborators", "admins", "createdBy", "joinRequests"})
  private Room room;

  @ManyToOne
  @JoinColumn(name = "userId")
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private User user;

  @Column(name = "status")
  private String status; // "pending", "approved", "rejected"

  @Column(name = "createdAt")
  private Instant createdAt;

  public RoomJoinRequest(Room room, User user) {
    this.room = room;
    this.user = user;
    this.status = "pending";
    this.createdAt = Instant.now();
  }
}

