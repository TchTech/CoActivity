package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull(message = "Sender ID is required")
    private Long senderId;

    @NotNull(message = "Content is required")
    @NotBlank(message = "Content cannot be blank")
    private String content;
}

