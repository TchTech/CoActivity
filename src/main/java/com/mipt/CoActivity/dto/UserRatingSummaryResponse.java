package com.mipt.CoActivity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRatingSummaryResponse {
    private Double average;
    private Long count;
}

