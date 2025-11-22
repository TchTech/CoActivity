package com.mipt.CoActivity.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoomChatResponse {
    private Long roomId;
    private List<ChatMessageResponse> messages;
}

