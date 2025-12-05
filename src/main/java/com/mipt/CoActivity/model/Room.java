package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private List<User> admins = new ArrayList<>();
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  @Column(name = "description")
  private String description;
  @Column(name = "category")
  private String category;
  @ManyToMany
  @JoinTable(
          name = "roomsCollaborators",
          joinColumns = @JoinColumn(name = "roomId"),
          inverseJoinColumns = @JoinColumn(name = "userId")
  )
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private List<User> collaborators = new ArrayList<>();
  @ManyToOne
  @JoinColumn(name = "interestTypeId")
  private InterestCategory interestType;
  @Column(name = "geoposition")
  private String geoposition;
  @Column(name = "location")
  private String location;
  @Column(name = "meetingTime")
  private Instant meetingTime;
  @Column(name = "endTime")
  private Instant endTime;
  @Column(name = "meetingType")
  private String meetingType;
  @ManyToOne
  @JoinColumn(name = "createdById")
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private User createdBy;
  @Column(name = "createdAt")
  private Instant createdAt;
  @Column(name = "maxCollaborators")
  private Integer maxCollaborators;
  
  @Column(name = "joinType")
  private String joinType = "open"; // "open" or "by_application"
  
  @Column(name = "is_default")
  private Boolean isDefault = false; // True for default room containing all users
  
  @Column(name = "is_closed")
  private Boolean isClosed = false; // If true, room is closed and no new requests should be accepted. Auto-closed when event date passes.
  
  @Column(name = "closed_at")
  private Instant closedAt; // Timestamp when room was closed (either manually or automatically).
  
  @ManyToOne
  @JoinColumn(name = "closed_by")
  @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
  private User closedBy; // Who closed it, if manually closed
  
  @OneToMany(mappedBy = "room")
  @JsonIgnoreProperties({"room", "post"})
  private List<RoomPostPin> pinnedPosts = new ArrayList<>();
  
  public Room(User createdBy, String name) {
    this.createdBy = createdBy;
    this.name = name;
    this.collaborators = new ArrayList<>();
    this.admins = new ArrayList<>();
    this.createdAt = Instant.now();
    this.joinType = "open";
    this.isDefault = false;
    this.isClosed = false;
  }
}
