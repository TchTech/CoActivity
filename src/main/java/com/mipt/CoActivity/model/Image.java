package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "images")
public class Image {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  
  @JsonIgnore
  @Column(name = "content", columnDefinition = "BYTEA")
  private byte[] content;
}
