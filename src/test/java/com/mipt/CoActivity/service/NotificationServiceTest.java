package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.NotificationData;
import com.mipt.CoActivity.model.Notification;
import com.mipt.CoActivity.model.NotificationDeduplicationLog;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.RoomNotificationSettings;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.model.UserSettings;
import com.mipt.CoActivity.repository.NotificationDeduplicationLogRepository;
import com.mipt.CoActivity.repository.NotificationRepository;
import com.mipt.CoActivity.repository.RoomNotificationSettingsRepository;
import com.mipt.CoActivity.repository.RoomRepository;
import com.mipt.CoActivity.repository.UserRepository;
import com.mipt.CoActivity.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private RoomNotificationSettingsRepository roomNotificationSettingsRepository;

    @Mock
    private NotificationDeduplicationLogRepository deduplicationLogRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private UserSettings userSettings;
    private Room testRoom;
    private RoomNotificationSettings roomSettings;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);

        userSettings = new UserSettings(testUser);
        userSettings.setMembershipRequestNotifications(true);
        userSettings.setMembershipDecisionNotifications(true);
        userSettings.setNotificationsEnabled(true);

        User creator = new User("creator", "creator@test.com", "password");
        creator.setId(2L);
        testRoom = new Room(creator, "Test Room");
        testRoom.setId(1L);

        roomSettings = new RoomNotificationSettings(testRoom);
        roomSettings.setMembershipRequestNotifications(true);
    }

    @Test
    void createNotification_Success_WithPreferencesEnabled() {
        // Arrange
        NotificationData data = NotificationData.builder()
                .roomId(1L)
                .requestId(1L)
                .requesterId(3L)
                .build();

        Notification savedNotification = new Notification();
        savedNotification.setId(1);
        savedNotification.setUser(testUser);
        savedNotification.setType("MEMBERSHIP_REQUEST");
        savedNotification.setTitle("Test");
        savedNotification.setContent("Test content");
        savedNotification.setData(data.toJson());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(userSettings));
        when(roomNotificationSettingsRepository.findByRoomId(1L)).thenReturn(Optional.of(roomSettings));
        when(deduplicationLogRepository.findByDeduplicationHashAndUser_IdAndNotificationType(any(), eq(1L), any()))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // Act
        Notification result = notificationService.createNotification(
                1L, "MEMBERSHIP_REQUEST", "Test", "Test content", data, 1L);

        // Assert
        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(deduplicationLogRepository, times(1)).save(any(NotificationDeduplicationLog.class));
    }

    @Test
    void createNotification_Skipped_WhenUserPreferenceDisabled() {
        // Arrange
        userSettings.setMembershipRequestNotifications(false);
        NotificationData data = NotificationData.builder()
                .roomId(1L)
                .requestId(1L)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(userSettings));

        // Act
        Notification result = notificationService.createNotification(
                1L, "MEMBERSHIP_REQUEST", "Test", "Test content", data, 1L);

        // Assert
        assertNull(result, "Notification should not be created when preference is disabled");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_Skipped_WhenRoomPreferenceDisabled() {
        // Arrange
        roomSettings.setMembershipRequestNotifications(false);
        NotificationData data = NotificationData.builder()
                .roomId(1L)
                .requestId(1L)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(userSettings));
        when(roomNotificationSettingsRepository.findByRoomId(1L)).thenReturn(Optional.of(roomSettings));

        // Act
        Notification result = notificationService.createNotification(
                1L, "MEMBERSHIP_REQUEST", "Test", "Test content", data, 1L);

        // Assert
        assertNull(result, "Notification should not be created when room preference is disabled");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_PreventsDuplicate() {
        // Arrange
        NotificationData data = NotificationData.builder()
                .roomId(1L)
                .requestId(1L)
                .build();

        NotificationDeduplicationLog existingLog = new NotificationDeduplicationLog("hash123", testUser, "MEMBERSHIP_REQUEST");
        existingLog.setCreatedAt(Instant.now().minusSeconds(30)); // 30 seconds ago (within 1 hour)

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(userSettings));
        when(roomNotificationSettingsRepository.findByRoomId(1L)).thenReturn(Optional.of(roomSettings));
        when(deduplicationLogRepository.findByDeduplicationHashAndUser_IdAndNotificationType(any(), eq(1L), eq("MEMBERSHIP_REQUEST")))
                .thenReturn(Optional.of(existingLog));

        // Act
        Notification result = notificationService.createNotification(
                1L, "MEMBERSHIP_REQUEST", "Test", "Test content", data, 1L);

        // Assert
        assertNull(result, "Duplicate notification should not be created");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_AllowsAfterDeduplicationWindow() {
        // Arrange
        NotificationData data = NotificationData.builder()
                .roomId(1L)
                .requestId(1L)
                .build();

        NotificationDeduplicationLog oldLog = new NotificationDeduplicationLog("hash123", testUser, "MEMBERSHIP_REQUEST");
        oldLog.setCreatedAt(Instant.now().minusSeconds(3700)); // More than 1 hour ago

        Notification savedNotification = new Notification();
        savedNotification.setId(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(userSettings));
        when(roomNotificationSettingsRepository.findByRoomId(1L)).thenReturn(Optional.of(roomSettings));
        when(deduplicationLogRepository.findByDeduplicationHashAndUser_IdAndNotificationType(any(), eq(1L), eq("MEMBERSHIP_REQUEST")))
                .thenReturn(Optional.of(oldLog));
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // Act
        Notification result = notificationService.createNotification(
                1L, "MEMBERSHIP_REQUEST", "Test", "Test content", data, 1L);

        // Assert
        assertNotNull(result, "Notification should be created after deduplication window");
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void createNotification_GeneratesCorrectDeduplicationHash() {
        // Arrange
        NotificationData data = NotificationData.builder()
                .roomId(1L)
                .requestId(1L)
                .build();

        Notification savedNotification = new Notification();
        savedNotification.setId(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(userSettings));
        when(roomNotificationSettingsRepository.findByRoomId(1L)).thenReturn(Optional.of(roomSettings));
        when(deduplicationLogRepository.findByDeduplicationHashAndUser_IdAndNotificationType(any(), eq(1L), any()))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // Act
        notificationService.createNotification(
                1L, "MEMBERSHIP_REQUEST", "Test", "Test content", data, 1L);

        // Assert - Verify hash is set
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();
        assertNotNull(captured.getDeduplicationHash());
        assertEquals(64, captured.getDeduplicationHash().length(), "MD5 hash should be 64 characters (hex)");
    }

    @Test
    void createNotificationForMultiple_RespectsPreferences() {
        // Arrange
        User user1 = new User("user1", "user1@test.com", "password");
        user1.setId(2L);
        UserSettings settings1 = new UserSettings(user1);
        settings1.setMembershipDecisionNotifications(true);

        User user2 = new User("user2", "user2@test.com", "password");
        user2.setId(3L);
        UserSettings settings2 = new UserSettings(user2);
        settings2.setMembershipDecisionNotifications(false); // Disabled

        NotificationData data = NotificationData.builder()
                .roomId(1L)
                .requestId(1L)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user2));
        when(userSettingsRepository.findByUserId(2L)).thenReturn(Optional.of(settings1));
        when(userSettingsRepository.findByUserId(3L)).thenReturn(Optional.of(settings2));
        when(deduplicationLogRepository.findByDeduplicationHashAndUser_IdAndNotificationType(any(), any(), any()))
                .thenReturn(Optional.empty());

        Notification savedNotification = new Notification();
        savedNotification.setId(1);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // Act
        int createdCount = notificationService.createNotificationForMultiple(
                List.of(2L, 3L), "MEMBERSHIP_APPROVED", "Approved", "Your request was approved", data, 1L);

        // Assert
        assertEquals(1, createdCount, "Only one notification should be created (user2 has preference disabled)");
        verify(notificationRepository, times(1)).save(any()); // Only for user1
    }

    @Test
    void cleanupOldNotifications_DeletesOldRecords() {
        // Arrange
        Notification oldNotification = new Notification();
        oldNotification.setId(1);
        oldNotification.setCreatedAt(Instant.now().minusSeconds(900000)); // ~10.4 days ago

        Notification recentNotification = new Notification();
        recentNotification.setId(2);
        recentNotification.setCreatedAt(Instant.now().minusSeconds(86400)); // 1 day ago

        when(notificationRepository.findAll()).thenReturn(List.of(oldNotification, recentNotification));

        // Act
        int deletedCount = notificationService.cleanupOldNotifications();

        // Assert
        assertTrue(deletedCount > 0);
        verify(notificationRepository, atLeastOnce()).delete(any(Notification.class));
    }

    @Test
    void cleanupDeduplicationLog_DeletesOldEntries() {
        // Arrange
        Instant cutoffTime = Instant.now().minusSeconds(3700); // More than 1 hour ago
        when(deduplicationLogRepository.deleteOlderThan(cutoffTime)).thenReturn(5);

        // Act
        int deletedCount = notificationService.cleanupDeduplicationLog();

        // Assert
        assertEquals(5, deletedCount);
        verify(deduplicationLogRepository, times(1)).deleteOlderThan(any(Instant.class));
    }
}

