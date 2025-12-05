package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a membership request.
 */
@Data
public class MembershipRequestRequest {
    /**
     * Optional message from requester (max 500 characters).
     */
    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}

