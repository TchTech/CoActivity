package com.mipt.CoActivity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomDetailsResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private Long creatorId;
    private String creatorName;
    private CreatorAvatar creatorAvatar;
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
    private List<RoomMemberInfo> members = new ArrayList<>();
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoomMemberInfo {
        private Long id;
        private String username;
        private String name;
        private Double rating;
        private MemberAvatar avatar;
        private Boolean isAdmin;
        
        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class MemberAvatar {
            private Integer id;
        }
    }
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreatorAvatar {
        private Integer id;
    }
}

