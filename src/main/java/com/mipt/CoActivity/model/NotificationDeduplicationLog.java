package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@NoArgsConstructor
@Entity
@Table(name = "notification_deduplication_log", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"deduplication_hash", "user_id", "notification_type"}
       ))
public class NotificationDeduplicationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deduplication_hash", length = 64, nullable = false)
    private String deduplicationHash;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
    private User user;

    @Column(name = "notification_type", length = 50, nullable = false)
    private String notificationType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public NotificationDeduplicationLog(String deduplicationHash, User user, String notificationType) {
        this.deduplicationHash = deduplicationHash;
        this.user = user;
        this.notificationType = notificationType;
        this.createdAt = Instant.now();
    }
}

