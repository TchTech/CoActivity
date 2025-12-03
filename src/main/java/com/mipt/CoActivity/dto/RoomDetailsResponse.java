package com.mipt.CoActivity.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class RoomDetailsResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private Long creatorId;
    private String creatorName;
    private String location;
    private Integer memberCount;
    private Integer pinnedPostCount;
    private Instant updatedAt;
    private Instant createdAt;
    private String meetingType;
    private Instant meetingTime;
    private Integer maxCollaborators;
    private String joinType;
    private Boolean isDefault;
}

