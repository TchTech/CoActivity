package com.mipt.CoActivity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TwoFactorLoginRequest {
    @NotNull(message = "Email is required")
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Password is required")
    @NotBlank(message = "Password cannot be blank")
    private String password;

    @NotNull(message = "Code is required")
    @NotBlank(message = "Code cannot be blank")
    @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
    private String code;
}

