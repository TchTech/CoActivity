package com.mipt.CoActivity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalLinkResponse {
    private Long id;
    private String platformName;
    private String label;
    private String url;
}

