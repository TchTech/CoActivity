package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    @NotNull(message = "Code is required")
    @NotBlank(message = "Code cannot be blank")
    @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
    private String code;
}

