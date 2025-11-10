package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;
@Data
public class Message {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne
  @JoinColumn(name = "roomId")
  private Room room;
  private String text;
  private Instant date;
  private Instant dateCreated;
  private Boolean isDeleted;
  @ManyToOne
  @JoinColumn(name = "authorId")
  private User author;
  public Message(Room room, User author, String text) {
    this.room = room;
    this.author = author;
    this.text = text;
    this.isDeleted = false;
    this.dateCreated = Instant.now();
  }
}
