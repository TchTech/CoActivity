package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to add an external link")
public class ExternalLinkRequest {
    @NotNull(message = "Platform name is required")
    @NotBlank(message = "Platform name cannot be blank")
    @Schema(description = "Platform name (e.g., Telegram, GitHub, LinkedIn)", example = "Telegram", required = true)
    private String platformName;

    @NotNull(message = "URL is required")
    @NotBlank(message = "URL cannot be blank")
    @Schema(description = "Link to profile", example = "https://t.me/username", required = true)
    private String url;
}

