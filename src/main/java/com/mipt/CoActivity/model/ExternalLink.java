package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@Table(name = "external_links")
public class ExternalLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userId")
    @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
    private User user;

    @Column(name = "platformName")
    private String platformName;

    @Column(name = "label")
    private String label; // Optional display label

    @Column(name = "url")
    private String url;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public ExternalLink(User user, String platformName, String url) {
        this.user = user;
        this.platformName = platformName;
        this.url = url;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}

