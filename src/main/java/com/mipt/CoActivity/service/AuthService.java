package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.LoginRequest;
import com.mipt.CoActivity.dto.LoginResponse;
import com.mipt.CoActivity.dto.RegisterRequest;
import com.mipt.CoActivity.model.User;

public interface AuthService {
    User registerNewUser(RegisterRequest request);
    LoginResponse loginUser(LoginRequest request);
}

