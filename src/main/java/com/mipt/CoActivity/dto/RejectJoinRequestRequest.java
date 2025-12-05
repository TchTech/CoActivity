package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for rejecting a room join request.
 */
@Data
public class RejectJoinRequestRequest {
    /**
     * ID of the admin rejecting the request (must be room creator or admin).
     */
    @NotNull(message = "Admin ID is required")
    private Long adminId;
    
    /**
     * Optional reason for rejection.
     */
    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    private String reason;
}

