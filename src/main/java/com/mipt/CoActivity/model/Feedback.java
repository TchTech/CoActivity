package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "feedbacks")
public class Feedback {
  @ManyToOne
  @JoinColumn(name = "authorId")
  private User author;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private String text;
  @ManyToOne
  @JoinColumn(name = "reviewedUserId")
  private User reviewedUser;
}
