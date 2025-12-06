package com.mipt.CoActivity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private NotificationCleanupService notificationCleanupService;

    @Test
    void testCleanupOldNotifications_Success() {
        // Given
        when(notificationService.cleanupOldNotifications()).thenReturn(10);

        // When
        notificationCleanupService.cleanupOldNotifications();

        // Then
        verify(notificationService, times(1)).cleanupOldNotifications();
    }

    @Test
    void testCleanupOldNotifications_HandlesException() {
        // Given
        when(notificationService.cleanupOldNotifications()).thenThrow(new RuntimeException("Database error"));

        // When
        notificationCleanupService.cleanupOldNotifications();

        // Then
        verify(notificationService, times(1)).cleanupOldNotifications();
        // Should not throw exception
    }

    @Test
    void testCleanupDeduplicationLog_Success() {
        // Given
        when(notificationService.cleanupDeduplicationLog()).thenReturn(5);

        // When
        notificationCleanupService.cleanupDeduplicationLog();

        // Then
        verify(notificationService, times(1)).cleanupDeduplicationLog();
    }

    @Test
    void testCleanupDeduplicationLog_HandlesException() {
        // Given
        when(notificationService.cleanupDeduplicationLog()).thenThrow(new RuntimeException("Database error"));

        // When
        notificationCleanupService.cleanupDeduplicationLog();

        // Then
        verify(notificationService, times(1)).cleanupDeduplicationLog();
        // Should not throw exception
    }

    @Test
    void testAutoCloseExpiredRooms_Success() {
        // Given
        doNothing().when(roomService).autoCloseExpiredRooms();

        // When
        notificationCleanupService.autoCloseExpiredRooms();

        // Then
        verify(roomService, times(1)).autoCloseExpiredRooms();
    }

    @Test
    void testAutoCloseExpiredRooms_HandlesException() {
        // Given
        doThrow(new RuntimeException("Database error")).when(roomService).autoCloseExpiredRooms();

        // When
        notificationCleanupService.autoCloseExpiredRooms();

        // Then
        verify(roomService, times(1)).autoCloseExpiredRooms();
        // Should not throw exception
    }
}

