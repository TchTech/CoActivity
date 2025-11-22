package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user_settings")
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "userId")
    private User user;

    @Column(name = "notificationsEnabled")
    private Boolean notificationsEnabled = true;

    @Column(name = "profileVisibility")
    private String profileVisibility = "public";

    @Column(name = "language")
    private String language = "ru";

    @Column(name = "emailNotifications")
    private Boolean emailNotifications = true;

    @Column(name = "pushNotifications")
    private Boolean pushNotifications = true;

    @Column(name = "messageNotifications")
    private Boolean messageNotifications = true;

    @Column(name = "friendRequestNotifications")
    private Boolean friendRequestNotifications = true;

    @Column(name = "eventNotifications")
    private Boolean eventNotifications = true;

    @Column(name = "roomRecommendations")
    private Boolean roomRecommendations = true;

    @Column(name = "friendsAccess")
    private String friendsAccess = "all";

    @Column(name = "dataLinksAccess")
    private String dataLinksAccess = "public";

    public UserSettings(User user) {
        this.user = user;
    }
}

