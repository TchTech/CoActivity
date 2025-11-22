package com.mipt.CoActivity.dto;

import lombok.Data;

@Data
public class RoomNotificationSettingsRequest {
    private Boolean invitationNotifications;
    private Boolean messageNotifications;
    private Boolean removalNotifications;
}

