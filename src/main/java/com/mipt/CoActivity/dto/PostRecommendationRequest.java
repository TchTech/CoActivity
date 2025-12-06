package com.mipt.CoActivity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRecommendationRequest {
    private List<String> userInterests;
    private List<PostData> posts;
    private List<Integer> subscribedUserIds;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostData {
        private Integer id;
        private String name;
        private String text;
        private String authorName;
        private Integer authorId; // ID автора поста для проверки подписок
        private List<String> authorInterests;
        private String roomName;
        private String roomCategory;
    }
}

