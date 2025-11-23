package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to update notification settings")
public class UpdateNotificationSettingsRequest {
    @Schema(description = "Enable/disable notifications about room invitations")
    private Boolean roomInvitationNotifications;

    @Schema(description = "Enable/disable notifications about messages")
    private Boolean messageNotifications;

    @Schema(description = "Enable/disable notifications about removal from rooms")
    private Boolean roomRemovalNotifications;
}

