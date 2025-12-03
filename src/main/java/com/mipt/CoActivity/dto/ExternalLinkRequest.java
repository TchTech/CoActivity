package com.mipt.CoActivity.dto;

import lombok.Data;

@Data
public class ExternalLinkRequest {
    private String platformName;
    private String label; // Optional display label
    private String url;
}
