package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to send a message in room chat")
public class SendMessageRequest {
    @NotNull(message = "Sender ID is required")
    @Schema(description = "ID of the user sending the message", example = "1", required = true)
    private Long senderId;

    @NotNull(message = "Content is required")
    @NotBlank(message = "Content cannot be blank")
    @Schema(description = "Message text content", example = "Hello everyone!", required = true)
    private String content;
}

