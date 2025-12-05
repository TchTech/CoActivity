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

    @Column(name = "membership_request_notifications")
    private Boolean membershipRequestNotifications = true; // User-level preference for receiving notifications about new membership requests (as room owner/admin)

    @Column(name = "membership_decision_notifications")
    private Boolean membershipDecisionNotifications = true; // User-level preference for receiving notifications about membership request decisions (as applicant)

    public UserSettings(User user) {
        this.user = user;
    }
}

