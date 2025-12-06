package com.mipt.CoActivity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRecommendationResponse {
    private List<PostData> recommendedPosts;
    private List<Double> similarityScores;
    private String message;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostData {
        private Integer id;
        private String name;
        private String text;
        private String authorName;
        private List<String> authorInterests;
        private String roomName;
        private String roomCategory;
    }
}

