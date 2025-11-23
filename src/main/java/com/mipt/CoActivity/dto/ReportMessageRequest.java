package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportMessageRequest {
    @NotNull(message = "Reporter ID is required")
    private Long reporterId;

    @NotNull(message = "Violation type is required")
    private String violationType;

    private String comment;
}

