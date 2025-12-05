package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RejectJoinRequestRequest {
    @NotNull(message = "Admin ID is required")
    private Long adminId;
    
    private String reason; // Optional reason for rejection
}

