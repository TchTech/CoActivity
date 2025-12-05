package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Data
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class Message {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne
  @JoinColumn(name = "roomId")
  @JsonIgnoreProperties({"collaborators", "admins", "createdBy", "messages"})
  private Room room;
  private String text;
  private Instant date;
  private Instant dateCreated;
  private Boolean isDeleted;
  @ManyToOne
  @JoinColumn(name = "authorId")
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private User author;
  public Message(Room room, User author, String text) {
    this.room = room;
    this.author = author;
    this.text = text;
    this.isDeleted = false;
    this.dateCreated = Instant.now();
  }
}
