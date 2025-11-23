package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to reject a join request")
public class RejectJoinRequestRequest {
    @NotNull(message = "Admin ID is required")
    @Schema(description = "ID of the administrator rejecting the request", example = "1", required = true)
    private Long adminId;
}

