package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "interests")
public class Interest {
  private String name;
  @ManyToOne
  @JoinColumn(name = "categoryId")
  private InterestCategory category;
  private String description;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
}
