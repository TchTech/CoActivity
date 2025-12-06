package com.mipt.CoActivity.dto;

import com.mipt.CoActivity.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private User user;
    private boolean emailVerificationRequired;
    private String message;
}

