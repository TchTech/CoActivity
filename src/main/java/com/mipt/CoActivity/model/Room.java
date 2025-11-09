package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Data
@Entity
@Table(name = "rooms")
public class Room {
  private List<User> admins;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  @Column(name = "description")
  private String description;
  @ManyToMany
  @JoinTable(
          name = "roomsCollaborators",
          joinColumns = @JoinColumn(name = "roomId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
  private List<User> collaborators;
  @Column(name = "interestType")
  private InterestCategory interestType;
  @Column(name = "geoposition")
  private String geoposition;
  @Column(name = "createdBy")
  private User createdBy;
  @Column(name = "createdAt")
  private Instant createdAt;
  @Column(name = "maxCollaborators")
  private Integer maxCollaborators;
  public Room(User createdBy, String name) {
    this.createdBy = createdBy;
    this.name = name;
    this.collaborators = new ArrayList<>();
    this.admins = new ArrayList<>();
    this.createdAt = Instant.now();
  }
}
