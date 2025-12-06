package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull(message = "Sender ID is required")
    private Long senderId;

    private String content; // Optional - can be empty if imageId is provided
    
    private Integer imageId; // Optional image ID
    
    // Custom validation: either content or imageId must be provided
    public boolean isValid() {
        return (content != null && !content.trim().isEmpty()) || imageId != null;
    }
}

