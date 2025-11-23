package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to report a message")
public class ReportMessageRequest {
    @NotNull(message = "Reporter ID is required")
    @Schema(description = "ID of the user reporting the message", example = "1", required = true)
    private Long reporterId;

    @NotNull(message = "Violation type is required")
    @Schema(description = "Type of violation (platform rules or room rules)", 
            example = "platformRules", 
            allowableValues = {"platformRules", "roomRules"},
            required = true)
    private String violationType;

    @Schema(description = "Optional comment to the report", example = "This message contains inappropriate content")
    private String comment;
}

