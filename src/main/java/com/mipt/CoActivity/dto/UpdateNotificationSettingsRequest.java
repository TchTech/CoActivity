package com.mipt.CoActivity.dto;

import lombok.Data;

@Data
public class UpdateNotificationSettingsRequest {
    private Boolean roomInvitationNotifications;
    private Boolean messageNotifications;
    private Boolean roomRemovalNotifications;
}

