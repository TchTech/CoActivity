package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing notification settings")
public class NotificationSettingsResponse {
    @Schema(description = "Notifications about room invitations")
    private Boolean roomInvitationNotifications;

    @Schema(description = "Notifications about new messages")
    private Boolean messageNotifications;

    @Schema(description = "Notifications about removal from rooms")
    private Boolean roomRemovalNotifications;

    @Schema(description = "Email notifications")
    private Boolean emailNotifications;

    @Schema(description = "Push notifications")
    private Boolean pushNotifications;
}

