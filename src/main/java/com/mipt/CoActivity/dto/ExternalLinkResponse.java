package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing external link information")
public class ExternalLinkResponse {
    @Schema(description = "Link ID", example = "1")
    private Long id;

    @Schema(description = "Platform name", example = "Telegram")
    private String platformName;

    @Schema(description = "Link URL", example = "https://t.me/username")
    private String url;
}

