package com.mipt.CoActivity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "room_post_pin")
public class RoomPostPin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    @JsonIgnoreProperties({"collaborators", "admins", "createdBy", "pinnedPosts"})
    private Room room;

    @ManyToOne
    @JoinColumn(name = "post_id")
    @JsonIgnoreProperties({"author", "likedUsers", "dislikedUsers", "comments", "room", "image"})
    private Post post;

    @ManyToOne
    @JoinColumn(name = "pinned_by")
    @JsonIgnoreProperties({"posts", "rooms", "interests", "feedbacks", "feedbacksAuthor", "subscriptions", "followers", "passwordHash"})
    private User pinnedBy;

    @Column(name = "pinned_at")
    private Instant pinnedAt = Instant.now();

    public RoomPostPin(Room room, Post post, User pinnedBy) {
        this.room = room;
        this.post = post;
        this.pinnedBy = pinnedBy;
        this.pinnedAt = Instant.now();
    }
}

