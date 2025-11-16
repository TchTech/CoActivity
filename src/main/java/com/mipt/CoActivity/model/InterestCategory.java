package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "interest_categories")
public class InterestCategory {
  private String name;
  private String description;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
}
