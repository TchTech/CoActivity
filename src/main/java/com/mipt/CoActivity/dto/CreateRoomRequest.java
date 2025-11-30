package com.mipt.CoActivity.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class CreateRoomRequest {
    private String description;
    private String category;
    private Integer maxCollaborators;
    private Instant meetingTime;
    private String meetingType; // "online" or "offline"
    private String location;
    private String joinType; // "open" or "application"
}

