package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmViolationRequest {
    @NotNull(message = "Admin ID is required")
    private Long adminId;

    @NotNull(message = "Sanction is required")
    private String sanction;
}

