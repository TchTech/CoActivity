package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RejectViolationRequest {
    @NotNull(message = "Admin ID is required")
    private Long adminId;
}

