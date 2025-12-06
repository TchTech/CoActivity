package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.model.*;
import com.mipt.CoActivity.service.UserService;
import com.mipt.CoActivity.service.UserRatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class UserControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRatingService userRatingService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private PersonalInfoResponse personalInfo;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);
        testUser.setName("Test User");

        personalInfo = new PersonalInfoResponse();
        personalInfo.setName("Test User");
        personalInfo.setEmail("test@example.com");
    }

    @Test
    void testGetUserProfile_Success() throws Exception {
        // Given
        when(userService.getUserProfile(1L)).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void testGetUserRating_Success() throws Exception {
        // Given
        when(userService.calculateUserRating(1L)).thenReturn(8.5);

        // When & Then
        mockMvc.perform(get("/users/1/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(8.5))
                .andExpect(jsonPath("$.hasRating").value(true));
    }

    @Test
    void testGetPersonalInfo_Success() throws Exception {
        // Given
        when(userService.getPersonalInfo(1L)).thenReturn(personalInfo);

        // When & Then
        mockMvc.perform(get("/users/1/profile/personal-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testUpdateUserName_Success() throws Exception {
        // Given
        UpdateNameRequest request = new UpdateNameRequest();
        request.setName("Updated Name");
        doNothing().when(userService).updateUserName(eq(1L), any(UpdateNameRequest.class));

        // When & Then
        mockMvc.perform(put("/users/1/profile/personal-info/name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateUserEmail_Success() throws Exception {
        // Given
        UpdateEmailRequest request = new UpdateEmailRequest();
        request.setEmail("newemail@example.com");
        doNothing().when(userService).updateUserEmail(eq(1L), any(UpdateEmailRequest.class));

        // When & Then
        mockMvc.perform(put("/users/1/profile/personal-info/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testViewUserProfile_Success() throws Exception {
        // Given
        when(userService.viewUserProfile(2L, 1L)).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/users/2/profile/view")
                .param("currentUserId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testGetUserRooms_Success() throws Exception {
        // Given
        List<Room> rooms = new ArrayList<>();
        when(userService.getUserRooms(1L)).thenReturn(rooms);

        // When & Then
        mockMvc.perform(get("/users/1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetUserSettings_Success() throws Exception {
        // Given
        UserSettings settings = new UserSettings();
        settings.setNotificationsEnabled(true);
        when(userService.getUserSettings(1L)).thenReturn(settings);

        // When & Then
        mockMvc.perform(get("/users/1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationsEnabled").value(true));
    }

    @Test
    void testGetUserNotificationSettings_Success() throws Exception {
        // Given
        NotificationSettingsResponse response = new NotificationSettingsResponse();
        response.setEmailNotifications(true);
        response.setPushNotifications(true);
        when(userService.getUserNotificationSettings(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/users/1/settings/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailNotifications").value(true));
    }

    @Test
    void testUpdateUserNotificationSettings_Success() throws Exception {
        // Given
        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest();
        request.setRoomInvitationNotifications(false);
        doNothing().when(userService).updateUserNotificationSettings(eq(1L), any(UpdateNotificationSettingsRequest.class));

        // When & Then
        mockMvc.perform(put("/users/1/settings/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetGeneralNotificationSettings_Success() throws Exception {
        // Given
        UserSettings settings = new UserSettings();
        settings.setNotificationsEnabled(true);
        when(userService.getUserSettings(1L)).thenReturn(settings);

        // When & Then
        mockMvc.perform(get("/users/1/settings/general-notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationsEnabled").value(true));
    }

    @Test
    void testUpdateGeneralNotificationSettings_Success() throws Exception {
        // Given
        GeneralNotificationSettingsRequest request = new GeneralNotificationSettingsRequest();
        request.setPushNotifications(true);
        UserSettings updatedSettings = new UserSettings();
        updatedSettings.setNotificationsEnabled(true);
        when(userService.updateGeneralNotificationSettings(eq(1L), any(GeneralNotificationSettingsRequest.class)))
                .thenReturn(updatedSettings);

        // When & Then
        mockMvc.perform(put("/users/1/settings/general-notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateRating_Success() throws Exception {
        // Given
        UserRatingRequest request = new UserRatingRequest();
        request.setScore(java.math.BigDecimal.valueOf(8.5));
        UserRating rating = new UserRating();
        when(userRatingService.createOrUpdateRating(eq(1L), eq(2L), any(UserRatingRequest.class))).thenReturn(rating);

        // When & Then
        mockMvc.perform(post("/users/1/rating")
                .param("raterUserId", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetRatingSummary_Success() throws Exception {
        // Given
        UserRatingSummaryResponse response = new UserRatingSummaryResponse(8.5, 10L);
        when(userRatingService.getRatingSummary(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/users/1/ratings/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(8.5))
                .andExpect(jsonPath("$.count").value(10));
    }
}

