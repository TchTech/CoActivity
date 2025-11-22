package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "room_notification_settings")
public class RoomNotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "roomId")
    private Room room;

    @Column(name = "invitationNotifications")
    private Boolean invitationNotifications = true;

    @Column(name = "messageNotifications")
    private Boolean messageNotifications = true;

    @Column(name = "removalNotifications")
    private Boolean removalNotifications = true;

    public RoomNotificationSettings(Room room) {
        this.room = room;
    }
}

