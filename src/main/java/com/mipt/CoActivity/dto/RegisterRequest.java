package com.mipt.CoActivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request for user registration")
public class RegisterRequest {
    @NotNull(message = "Name is required")
    @NotBlank(message = "Name cannot be blank")
    @Schema(description = "User name", example = "John Doe", required = true)
    private String name;

    @NotNull(message = "Email is required")
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Schema(description = "User email", example = "john.doe@example.com", required = true)
    private String email;

    @NotNull(message = "Password is required")
    @NotBlank(message = "Password cannot be blank")
    @Schema(description = "User password", required = true)
    private String password;
}

