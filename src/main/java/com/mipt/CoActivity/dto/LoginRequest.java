package com.mipt.CoActivity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    // Может быть как email, так и username
    @NotNull(message = "Login is required")
    @NotBlank(message = "Login cannot be blank")
    @JsonProperty("login")
    @JsonAlias({"email", "username"}) // Поддержка обратной совместимости
    private String login; // Изменено с email на login для ясности

    @NotNull(message = "Password is required")
    @NotBlank(message = "Password cannot be blank")
    @JsonProperty("password")
    private String password;
}

