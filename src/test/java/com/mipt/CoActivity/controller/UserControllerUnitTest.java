package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.ForbiddenException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testUserId;
    private NotificationSettingsResponse notificationSettingsResponse;
    private ExternalLinkResponse externalLinkResponse;

    @BeforeEach
    void setUp() {
        testUserId = 1L;

        notificationSettingsResponse = NotificationSettingsResponse.builder()
                .roomInvitationNotifications(true)
                .messageNotifications(true)
                .roomRemovalNotifications(false)
                .emailNotifications(true)
                .pushNotifications(true)
                .build();

        externalLinkResponse = ExternalLinkResponse.builder()
                .id(1L)
                .platformName("Telegram")
                .url("https://t.me/username")
                .build();
    }

    @Test
    void shouldLogoutUserSuccessfully() throws Exception {
        doNothing().when(userService).logoutUser(testUserId);

        mockMvc.perform(post("/users/{id}/profile/logout", testUserId))
                .andExpect(status().isOk());

        verify(userService, times(1)).logoutUser(testUserId);
    }

    @Test
    void shouldReturn404WhenLoggingOutNonExistentUser() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).logoutUser(testUserId);

        mockMvc.perform(post("/users/{id}/profile/logout", testUserId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    void shouldGetUserNotificationSettingsSuccessfully() throws Exception {
        when(userService.getUserNotificationSettings(testUserId))
                .thenReturn(notificationSettingsResponse);

        mockMvc.perform(get("/users/{userId}/settings/notifications", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomInvitationNotifications").value(true))
                .andExpect(jsonPath("$.messageNotifications").value(true))
                .andExpect(jsonPath("$.roomRemovalNotifications").value(false))
                .andExpect(jsonPath("$.emailNotifications").value(true))
                .andExpect(jsonPath("$.pushNotifications").value(true));
    }

    @Test
    void shouldReturn404WhenGettingNotificationSettingsForNonExistentUser() throws Exception {
        when(userService.getUserNotificationSettings(testUserId))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/{userId}/settings/notifications", testUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateUserNotificationSettingsSuccessfully() throws Exception {
        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest();
        request.setRoomInvitationNotifications(false);
        request.setMessageNotifications(true);
        request.setRoomRemovalNotifications(true);

        doNothing().when(userService).updateUserNotificationSettings(eq(testUserId), any(UpdateNotificationSettingsRequest.class));

        mockMvc.perform(put("/users/{userId}/settings/notifications", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService, times(1)).updateUserNotificationSettings(eq(testUserId), any(UpdateNotificationSettingsRequest.class));
    }

    @Test
    void shouldReturn400WhenUpdatingNotificationSettingsWithInvalidData() throws Exception {
        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest();

        doNothing().when(userService).updateUserNotificationSettings(eq(testUserId), any(UpdateNotificationSettingsRequest.class));

        mockMvc.perform(put("/users/{userId}/settings/notifications", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenUpdatingNotificationSettingsForNonExistentUser() throws Exception {
        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest();
        request.setMessageNotifications(true);

        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).updateUserNotificationSettings(eq(testUserId), any(UpdateNotificationSettingsRequest.class));

        mockMvc.perform(put("/users/{userId}/settings/notifications", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetUserExternalLinksSuccessfully() throws Exception {
        List<ExternalLinkResponse> links = Arrays.asList(
                externalLinkResponse,
                ExternalLinkResponse.builder()
                        .id(2L)
                        .platformName("GitHub")
                        .url("https://github.com/username")
                        .build()
        );

        when(userService.getUserExternalLinks(testUserId)).thenReturn(links);

        mockMvc.perform(get("/users/{id}/profile/external-links", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].platformName").value("Telegram"))
                .andExpect(jsonPath("$[0].url").value("https://t.me/username"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].platformName").value("GitHub"));
    }

    @Test
    void shouldReturn404WhenGettingExternalLinksForNonExistentUser() throws Exception {
        when(userService.getUserExternalLinks(testUserId))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/{id}/profile/external-links", testUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAddExternalLinkSuccessfully() throws Exception {
        ExternalLinkRequest request = new ExternalLinkRequest();
        request.setPlatformName("LinkedIn");
        request.setUrl("https://linkedin.com/in/username");

        ExternalLinkResponse response = ExternalLinkResponse.builder()
                .id(3L)
                .platformName("LinkedIn")
                .url("https://linkedin.com/in/username")
                .build();

        when(userService.addExternalLink(eq(testUserId), any(ExternalLinkRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/users/{id}/profile/external-links", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.platformName").value("LinkedIn"))
                .andExpect(jsonPath("$.url").value("https://linkedin.com/in/username"));
    }

    @Test
    void shouldReturn400WhenAddingExternalLinkWithInvalidUrl() throws Exception {
        ExternalLinkRequest request = new ExternalLinkRequest();
        request.setPlatformName("Telegram");
        request.setUrl("invalid-url");

        when(userService.addExternalLink(eq(testUserId), any(ExternalLinkRequest.class)))
                .thenThrow(new BadRequestException("URL must be valid (start with http:// or https://)"));

        mockMvc.perform(post("/users/{id}/profile/external-links", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("URL must be valid (start with http:// or https://)"));
    }

    @Test
    void shouldReturn400WhenAddingExternalLinkWithEmptyUrl() throws Exception {
        ExternalLinkRequest request = new ExternalLinkRequest();
        request.setPlatformName("Telegram");
        request.setUrl("");

        when(userService.addExternalLink(eq(testUserId), any(ExternalLinkRequest.class)))
                .thenThrow(new BadRequestException("URL cannot be empty"));

        mockMvc.perform(post("/users/{id}/profile/external-links", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenAddingExternalLinkForNonExistentUser() throws Exception {
        ExternalLinkRequest request = new ExternalLinkRequest();
        request.setPlatformName("Telegram");
        request.setUrl("https://t.me/username");

        when(userService.addExternalLink(eq(testUserId), any(ExternalLinkRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/users/{id}/profile/external-links", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteExternalLinkSuccessfully() throws Exception {
        Long linkId = 1L;
        doNothing().when(userService).deleteExternalLink(testUserId, linkId);

        mockMvc.perform(delete("/users/{id}/profile/external-links/{linkId}", testUserId, linkId))
                .andExpect(status().isOk());

        verify(userService, times(1)).deleteExternalLink(testUserId, linkId);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentExternalLink() throws Exception {
        Long linkId = 999L;
        doThrow(new ResourceNotFoundException("External link not found"))
                .when(userService).deleteExternalLink(testUserId, linkId);

        mockMvc.perform(delete("/users/{id}/profile/external-links/{linkId}", testUserId, linkId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("External link not found"));
    }

    @Test
    void shouldReturn403WhenDeletingExternalLinkOfAnotherUser() throws Exception {
        Long linkId = 1L;
        doThrow(new ForbiddenException("You don't have permission to delete this link"))
                .when(userService).deleteExternalLink(testUserId, linkId);

        mockMvc.perform(delete("/users/{id}/profile/external-links/{linkId}", testUserId, linkId))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You don't have permission to delete this link"));
    }

    @Test
    void shouldDeserializeExternalLinkRequestCorrectly() throws Exception {
        ExternalLinkRequest request = new ExternalLinkRequest();
        request.setPlatformName("GitHub");
        request.setUrl("https://github.com/user");

        ExternalLinkResponse response = ExternalLinkResponse.builder()
                .id(1L)
                .platformName("GitHub")
                .url("https://github.com/user")
                .build();

        when(userService.addExternalLink(eq(testUserId), any(ExternalLinkRequest.class)))
                .thenReturn(response);

        String jsonRequest = """
                {
                    "platformName": "GitHub",
                    "url": "https://github.com/user"
                }
                """;

        mockMvc.perform(post("/users/{id}/profile/external-links", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platformName").value("GitHub"))
                .andExpect(jsonPath("$.url").value("https://github.com/user"));
    }

    @Test
    void shouldSerializeNotificationSettingsResponseCorrectly() throws Exception {
        when(userService.getUserNotificationSettings(testUserId))
                .thenReturn(notificationSettingsResponse);

        mockMvc.perform(get("/users/{userId}/settings/notifications", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomInvitationNotifications").exists())
                .andExpect(jsonPath("$.messageNotifications").exists())
                .andExpect(jsonPath("$.roomRemovalNotifications").exists())
                .andExpect(jsonPath("$.emailNotifications").exists())
                .andExpect(jsonPath("$.pushNotifications").exists());
    }
}


