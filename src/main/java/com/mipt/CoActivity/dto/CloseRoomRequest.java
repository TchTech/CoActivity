package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for closing a room.
 */
@Data
public class CloseRoomRequest {
    /**
     * ID of the user closing the room (must be room creator or admin).
     */
    @NotNull(message = "User ID is required")
    private Long userId;
}

