package com.mipt.CoActivity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {
    private Long senderId;
    private String content;
    private Instant timestamp;
    private String senderName;
    private SenderAvatar senderAvatar;
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SenderAvatar {
        private Integer id;
    }
}

