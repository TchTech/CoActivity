package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {
  @ManyToMany
  @JoinTable(
          name = "room_admins",
          joinColumns = @JoinColumn(name = "roomId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
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
  @ManyToOne
  @JoinColumn(name = "interestTypeId")
  private InterestCategory interestType;
  @Column(name = "geoposition")
  private String geoposition;
  @ManyToOne
  @JoinColumn(name = "createdById")
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
