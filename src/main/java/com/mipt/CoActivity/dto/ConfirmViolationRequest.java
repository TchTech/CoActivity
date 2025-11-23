package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to confirm a message violation")
public class ConfirmViolationRequest {
    @NotNull(message = "Admin ID is required")
    @Schema(description = "ID of the administrator confirming the violation", example = "1", required = true)
    private Long adminId;

    @NotNull(message = "Sanction is required")
    @Schema(description = "Type of sanction", 
            example = "warning", 
            allowableValues = {"warning", "kick", "ban"},
            required = true)
    private String sanction;
}

