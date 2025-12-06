package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomRatingNotificationSchedulerTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RoomRatingNotificationScheduler scheduler;

    private Room testRoom;
    private User creator;
    private User participant1;
    private User participant2;

    @BeforeEach
    void setUp() {
        creator = new User("creator", "creator@example.com", "password");
        creator.setId(1L);
        creator.setName("Creator Name");

        participant1 = new User("participant1", "p1@example.com", "password");
        participant1.setId(2L);

        participant2 = new User("participant2", "p2@example.com", "password");
        participant2.setId(3L);

        testRoom = new Room(creator, "Test Room");
        testRoom.setId(1L);
        testRoom.setEndTime(Instant.now().minusSeconds(1800)); // 30 minutes ago
        testRoom.setCollaborators(new ArrayList<>(List.of(creator, participant1, participant2)));
    }

    @Test
    void testSendRatingNotificationsForEndedRooms_SendsNotifications() {
        // Given
        List<Room> endedRooms = List.of(testRoom);
        when(roomRepository.findAll()).thenReturn(endedRooms);
        doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any());

        // When
        scheduler.sendRatingNotificationsForEndedRooms();

        // Then
        // Should send notifications to participant1 and participant2 (not creator)
        verify(notificationService, times(2)).createNotification(any(), eq("RATE_ROOM_CREATOR"), any(), any(), any());
    }

    @Test
    void testSendRatingNotificationsForEndedRooms_SkipsRoomWithoutCreator() {
        // Given
        testRoom.setCreatedBy(null);
        List<Room> endedRooms = List.of(testRoom);
        when(roomRepository.findAll()).thenReturn(endedRooms);

        // When
        scheduler.sendRatingNotificationsForEndedRooms();

        // Then
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void testSendRatingNotificationsForEndedRooms_SkipsRoomWithoutCollaborators() {
        // Given
        testRoom.setCollaborators(new ArrayList<>());
        List<Room> endedRooms = List.of(testRoom);
        when(roomRepository.findAll()).thenReturn(endedRooms);

        // When
        scheduler.sendRatingNotificationsForEndedRooms();

        // Then
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void testSendRatingNotificationsForEndedRooms_HandlesException() {
        // Given
        List<Room> endedRooms = List.of(testRoom);
        when(roomRepository.findAll()).thenReturn(endedRooms);
        doThrow(new RuntimeException("Notification error")).when(notificationService)
                .createNotification(any(), any(), any(), any(), any());

        // When
        scheduler.sendRatingNotificationsForEndedRooms();

        // Then
        // Should not throw exception, just log error
        verify(notificationService, atLeastOnce()).createNotification(any(), any(), any(), any(), any());
    }
}

