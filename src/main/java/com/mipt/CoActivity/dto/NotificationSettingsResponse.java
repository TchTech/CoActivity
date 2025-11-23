package com.mipt.CoActivity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsResponse {
    private Boolean roomInvitationNotifications;
    private Boolean messageNotifications;
    private Boolean roomRemovalNotifications;
    private Boolean emailNotifications;
    private Boolean pushNotifications;
}

