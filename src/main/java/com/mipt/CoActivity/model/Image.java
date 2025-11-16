package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "images")
public class Image {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Lob
  @Column(columnDefinition = "BYTEA")
  private byte[] content;
}
