package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Notification;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = NotificationController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private List<Notification> testNotifications;

    @BeforeEach
    void setUp() {
        testNotifications = new ArrayList<>();
        Notification notification = new Notification();
        notification.setId(1);
        User user = new User("testuser", "test@example.com", "password");
        user.setId(1L);
        notification.setUser(user);
        notification.setType("MEMBERSHIP_REQUEST");
        notification.setTitle("Test Notification");
        notification.setContent("Test content");
        testNotifications.add(notification);
    }

    @Test
    void testGetUserNotifications_Success() throws Exception {
        // Given
        when(notificationService.getUserNotifications(1L)).thenReturn(testNotifications);

        // When & Then
        mockMvc.perform(get("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetUnreadNotifications_Success() throws Exception {
        // Given
        when(notificationService.getUnreadNotifications(1L)).thenReturn(testNotifications);

        // When & Then
        mockMvc.perform(get("/api/notifications/1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetUnreadCount_Success() throws Exception {
        // Given
        when(notificationService.getUnreadCount(1L)).thenReturn(5L);

        // When & Then
        mockMvc.perform(get("/api/notifications/1/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void testMarkAsRead_Success() throws Exception {
        // Given
        doNothing().when(notificationService).markAsRead(1L, 1L);

        // When & Then
        mockMvc.perform(post("/api/notifications/1/read")
                .param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void testMarkAllAsRead_Success() throws Exception {
        // Given
        doNothing().when(notificationService).markAllAsRead(1L);

        // When & Then
        mockMvc.perform(post("/api/notifications/1/read-all"))
                .andExpect(status().isOk());
    }

    @Test
    void testMarkNotificationsAsRead_Success() throws Exception {
        // Given
        List<Integer> notificationIds = List.of(1, 2, 3);
        doNothing().when(notificationService).markNotificationsAsRead(notificationIds, 1L);

        // When & Then
        mockMvc.perform(post("/api/notifications/mark-read")
                .param("userId", "1")
                .contentType("application/json")
                .content("[1,2,3]"))
                .andExpect(status().isOk());
    }

    @Test
    void testDismissNotification_Success() throws Exception {
        // Given
        doNothing().when(notificationService).dismissNotification(1, 1L);

        // When & Then
        mockMvc.perform(delete("/api/notifications/1")
                .param("userId", "1"))
                .andExpect(status().isOk());
    }
}

