package com.mipt.CoActivity.dto;

import com.mipt.CoActivity.model.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedPostsResponse {
    private List<Post> posts;
    private List<Double> similarityScores;
    private String message;
}

