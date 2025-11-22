package com.mipt.CoActivity.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ChatMessageResponse {
    private Long senderId;
    private String content;
    private Instant timestamp;
}

