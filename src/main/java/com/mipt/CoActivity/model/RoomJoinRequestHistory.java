package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "room_join_request_history")
public class RoomJoinRequestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnoreProperties({"room", "user", "responder"})
    private RoomJoinRequest request;

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "new_status", length = 50, nullable = false)
    private String newStatus;

    @ManyToOne
    @JoinColumn(name = "changed_by")
    @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
    private User changedBy;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public RoomJoinRequestHistory(RoomJoinRequest request, String previousStatus, String newStatus, User changedBy) {
        this.request = request;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = Instant.now();
    }

    public RoomJoinRequestHistory(RoomJoinRequest request, String previousStatus, String newStatus, User changedBy, String changeReason) {
        this.request = request;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changeReason = changeReason;
        this.changedAt = Instant.now();
    }
}

