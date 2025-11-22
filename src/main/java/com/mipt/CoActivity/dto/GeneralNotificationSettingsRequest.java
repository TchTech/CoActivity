package com.mipt.CoActivity.dto;

import lombok.Data;

@Data
public class GeneralNotificationSettingsRequest {
    private Boolean emailNotifications;
    private Boolean pushNotifications;
}

