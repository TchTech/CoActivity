package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.LoginRequest;
import com.mipt.CoActivity.dto.LoginResponse;
import com.mipt.CoActivity.dto.RegisterRequest;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.UnauthorizedException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class AuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "hashedPassword");
        testUser.setId(1L);
        testUser.setName("Test User");

        registerRequest = new RegisterRequest();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setLogin("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        when(authService.registerNewUser(any(RegisterRequest.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldReturn400WhenEmailAlreadyExists() throws Exception {
        when(authService.registerNewUser(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Email already exists"));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already exists"));
    }

    @Test
    void shouldReturn400WhenRegisterRequestIsInvalid() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setName("");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginUserSuccessfully() throws Exception {
        LoginResponse loginResponse = LoginResponse.builder()
                .token("jwt-token-123")
                .userId(1L)
                .build();

        when(authService.loginUser(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void shouldReturn401WhenLoginCredentialsAreInvalid() throws Exception {
        when(authService.loginUser(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void shouldReturn401WhenUserNotFound() throws Exception {
        when(authService.loginUser(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        LoginRequest invalidLogin = new LoginRequest();
        invalidLogin.setLogin("nonexistent@example.com");
        invalidLogin.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDeserializeLoginRequestCorrectly() throws Exception {
        LoginResponse loginResponse = LoginResponse.builder()
                .token("test-token")
                .userId(1L)
                .build();

        when(authService.loginUser(any(LoginRequest.class))).thenReturn(loginResponse);

        String jsonRequest = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void shouldDeserializeRegisterRequestCorrectly() throws Exception {
        when(authService.registerNewUser(any(RegisterRequest.class))).thenReturn(testUser);

        String jsonRequest = """
                {
                    "name": "Test User",
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}


