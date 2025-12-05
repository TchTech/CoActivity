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
  private String status; // "pending", "approved", "rejected", "cancelled"

  @Column(name = "message", columnDefinition = "TEXT")
  private String message; // Optional message from requester

  @Column(name = "createdAt")
  private Instant createdAt;

  @Column(name = "respondedAt")
  private Instant respondedAt;

  @ManyToOne
  @JoinColumn(name = "responderId")
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private User responder; // Who approved/rejected

  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason; // Optional reason for rejection, provided by admin

  @Column(name = "last_rejected_at")
  private Instant lastRejectedAt; // Timestamp of last rejection. Used to enforce 5-minute cooldown before user can reapply.

  public RoomJoinRequest(Room room, User user) {
    this.room = room;
    this.user = user;
    this.status = "pending";
    this.createdAt = Instant.now();
  }
}

