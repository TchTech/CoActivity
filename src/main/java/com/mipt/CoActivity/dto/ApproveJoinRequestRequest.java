package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for approving a room join request.
 */
@Data
public class ApproveJoinRequestRequest {
    /**
     * ID of the admin approving the request (must be room creator or admin).
     */
    @NotNull(message = "Admin ID is required")
    private Long adminId;
}

