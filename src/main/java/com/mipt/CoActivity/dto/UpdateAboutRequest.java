package com.mipt.CoActivity.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class UpdateAboutRequest {
    @Size(max = 2000, message = "About text must not exceed 2000 characters")
    private String about;
}

