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
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {
  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
  private static final long DEDUPLICATION_WINDOW_HOURS = 1;

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final UserSettingsRepository userSettingsRepository;
  private final RoomNotificationSettingsRepository roomNotificationSettingsRepository;
  private final NotificationDeduplicationLogRepository deduplicationLogRepository;
  private final RoomRepository roomRepository;

  @Autowired
  public NotificationService(
          NotificationRepository notificationRepository,
          UserRepository userRepository,
          UserSettingsRepository userSettingsRepository,
          RoomNotificationSettingsRepository roomNotificationSettingsRepository,
          NotificationDeduplicationLogRepository deduplicationLogRepository,
          RoomRepository roomRepository) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.userSettingsRepository = userSettingsRepository;
    this.roomNotificationSettingsRepository = roomNotificationSettingsRepository;
    this.deduplicationLogRepository = deduplicationLogRepository;
    this.roomRepository = roomRepository;
  }

  public List<Notification> getUserNotifications(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  public List<Notification> getUnreadNotifications(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
  }

  public long getUnreadCount(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  @Transactional
  public void markAsRead(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    
    if (!notification.getUser().getId().equals(userId)) {
      throw new com.mipt.CoActivity.exception.ForbiddenException("You don't have permission to modify this notification");
    }
    
    notification.setIsRead(true);
    notificationRepository.save(notification);
  }

  @Transactional
  public void markAllAsRead(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    for (Notification notification : unreadNotifications) {
      notification.setIsRead(true);
      notificationRepository.save(notification);
    }
  }

  /**
   * Create a notification with preference checking and deduplication.
   * 
   * This method:
   * 1. Checks user notification preferences
   * 2. Checks room-level notification settings (if roomId provided)
   * 3. Generates deduplication hash
   * 4. Checks for duplicates within the time window
   * 5. Creates notification only if allowed and not duplicate
   * 
   * @param userId The user to receive the notification
   * @param type The notification type (e.g., "MEMBERSHIP_REQUEST")
   * @param title The notification title
   * @param content The notification content
   * @param data The notification data (can be NotificationData object or JSON string)
   * @param roomId Optional room ID for room-level preference checking
   * @return The created Notification, or null if notification was suppressed
   */
  @Transactional
  public Notification createNotification(Long userId, String type, String title, String content, Object data, Long roomId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Convert data to JSON string if it's a NotificationData object
    String dataJson;
    if (data instanceof NotificationData) {
      dataJson = ((NotificationData) data).toJson();
    } else if (data instanceof String) {
      dataJson = (String) data;
    } else {
      dataJson = null;
    }

    // Check notification preferences
    if (!shouldCreateNotification(userId, type, roomId)) {
      logger.debug("Notification suppressed for user {} type {} due to preferences", userId, type);
      return null; // Do not create notification record if preferences disabled
    }

    // Generate deduplication hash
    String deduplicationHash = generateDeduplicationHash(userId, type, dataJson);

    // Check for duplicates
    if (isDuplicate(deduplicationHash, userId, type)) {
      logger.debug("Duplicate notification prevented for user {} type {} hash {}", userId, type, deduplicationHash);
      return null; // Do not create duplicate notification
    }

    // Create notification
    Notification notification = new Notification();
    notification.setUser(user);
    notification.setType(type);
    notification.setTitle(title);
    notification.setContent(content);
    notification.setData(dataJson);
    notification.setDeduplicationHash(deduplicationHash);
    notification.setIsRead(false);
    notification.setCreatedAt(Instant.now());

    Notification savedNotification = notificationRepository.save(notification);

    // Log deduplication entry
    logDeduplication(deduplicationHash, user, type);

    logger.debug("Created notification {} for user {} type {}", savedNotification.getId(), userId, type);
    return savedNotification;
  }

  /**
   * Legacy method for backward compatibility.
   * Converts String data to NotificationData and calls the enhanced method.
   */
  @Transactional
  public Notification createNotification(Long userId, String type, String title, String content, String data) {
    return createNotification(userId, type, title, content, data, null);
  }

  /**
   * Create notifications for multiple users.
   * Useful for bulk notifications (e.g., when room closes, notify all applicants).
   * 
   * @param userIds List of user IDs to notify
   * @param type Notification type
   * @param title Notification title
   * @param content Notification content
   * @param data Notification data
   * @param roomId Optional room ID for preference checking
   * @return Number of notifications actually created (after preference filtering and deduplication)
   */
  @Transactional
  public int createNotificationForMultiple(List<Long> userIds, String type, String title, String content, Object data, Long roomId) {
    int createdCount = 0;
    for (Long userId : userIds) {
      try {
        Notification notification = createNotification(userId, type, title, content, data, roomId);
        if (notification != null) {
          createdCount++;
        }
      } catch (Exception e) {
        logger.warn("Failed to create notification for user {}: {}", userId, e.getMessage());
      }
    }
    logger.info("Created {} notifications out of {} users for type {}", createdCount, userIds.size(), type);
    return createdCount;
  }

  @Transactional
  public void markNotificationsAsRead(List<Integer> notificationIds, Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    for (Integer notificationId : notificationIds) {
      Notification notification = notificationRepository.findById(notificationId)
          .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
      
      if (!notification.getUser().getId().equals(userId)) {
        throw new com.mipt.CoActivity.exception.ForbiddenException("You don't have permission to modify this notification");
      }
      
      notification.setIsRead(true);
      notificationRepository.save(notification);
    }
  }

  @Transactional
  public void dismissNotification(Integer notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    
    if (!notification.getUser().getId().equals(userId)) {
      throw new com.mipt.CoActivity.exception.ForbiddenException("You don't have permission to dismiss this notification");
    }
    
    notificationRepository.delete(notification);
  }

  // ========== Private Helper Methods ==========

  /**
   * Check if notification should be created based on user and room preferences.
   * 
   * Rules:
   * - Check user-level preferences first
   * - If roomId provided, check room-level preferences (overrides user-level)
   * - If any preference is false, do not create notification
   * 
   * @param userId User ID
   * @param type Notification type
   * @param roomId Optional room ID
   * @return true if notification should be created, false otherwise
   */
  private boolean shouldCreateNotification(Long userId, String type, Long roomId) {
    // Get user settings
    Optional<UserSettings> userSettingsOpt = userSettingsRepository.findByUserId(userId);
    if (userSettingsOpt.isEmpty()) {
      // Default to enabled if no settings found
      return true;
    }

    UserSettings userSettings = userSettingsOpt.get();

    // Check user-level preferences based on notification type
    boolean userPreferenceEnabled = true;
    if (isMembershipRequestType(type)) {
      // For MEMBERSHIP_REQUEST: check membership_request_notifications (for room owners/admins)
      userPreferenceEnabled = userSettings.getMembershipRequestNotifications() != null
              && userSettings.getMembershipRequestNotifications();
    } else if (isMembershipDecisionType(type)) {
      // For MEMBERSHIP_APPROVED/REJECTED: check membership_decision_notifications (for applicants)
      userPreferenceEnabled = userSettings.getMembershipDecisionNotifications() != null
              && userSettings.getMembershipDecisionNotifications();
    } else {
      // For other types, check general notificationsEnabled
      userPreferenceEnabled = userSettings.getNotificationsEnabled() == null
              || userSettings.getNotificationsEnabled();
    }

    if (!userPreferenceEnabled) {
      return false;
    }

    // Check room-level preferences if roomId provided
    if (roomId != null && isMembershipRequestType(type)) {
      Optional<RoomNotificationSettings> roomSettingsOpt = roomNotificationSettingsRepository.findByRoomId(roomId);
      if (roomSettingsOpt.isPresent()) {
        RoomNotificationSettings roomSettings = roomSettingsOpt.get();
        boolean roomPreferenceEnabled = roomSettings.getMembershipRequestNotifications() == null
                || roomSettings.getMembershipRequestNotifications();
        // Room-level setting overrides user-level
        return roomPreferenceEnabled;
      }
    }

    return true;
  }

  /**
   * Check if notification type is a membership request type (for room owners/admins).
   */
  private boolean isMembershipRequestType(String type) {
    return "MEMBERSHIP_REQUEST".equals(type);
  }

  /**
   * Check if notification type is a membership decision type (for applicants).
   */
  private boolean isMembershipDecisionType(String type) {
    return "MEMBERSHIP_APPROVED".equals(type)
            || "MEMBERSHIP_REJECTED".equals(type)
            || "MEMBERSHIP_CANCELLED".equals(type)
            || "ROOM_CLOSED".equals(type)
            || "ROOM_CAPACITY_REACHED".equals(type)
            || "USER_BANNED".equals(type);
  }

  /**
   * Generate MD5 hash for deduplication.
   * Hash = MD5(userId + type + data)
   */
  private String generateDeduplicationHash(Long userId, String type, String data) {
    try {
      String input = userId + "|" + type + "|" + (data != null ? data : "");
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
      
      // Convert to hex string
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      logger.error("Failed to generate deduplication hash: {}", e.getMessage(), e);
      // Fallback: use simple hash
      return String.valueOf((userId + type + (data != null ? data : "")).hashCode());
    }
  }

  /**
   * Check if notification is a duplicate within the time window.
   */
  private boolean isDuplicate(String deduplicationHash, Long userId, String type) {
    Optional<NotificationDeduplicationLog> existingLog = deduplicationLogRepository
            .findByDeduplicationHashAndUser_IdAndNotificationType(deduplicationHash, userId, type);
    
    if (existingLog.isPresent()) {
      NotificationDeduplicationLog log = existingLog.get();
      Instant logTime = log.getCreatedAt();
      Instant now = Instant.now();
      
      // Check if within 1 hour window
      long hoursSinceLog = java.time.Duration.between(logTime, now).toHours();
      if (hoursSinceLog < DEDUPLICATION_WINDOW_HOURS) {
        return true; // Duplicate found within window
      }
    }
    
    return false; // No duplicate
  }

  /**
   * Log deduplication entry for future duplicate checks.
   */
  private void logDeduplication(String deduplicationHash, User user, String type) {
    try {
      NotificationDeduplicationLog log = new NotificationDeduplicationLog(deduplicationHash, user, type);
      deduplicationLogRepository.save(log);
    } catch (Exception e) {
      // Log but don't fail notification creation
      logger.warn("Failed to log deduplication entry: {}", e.getMessage());
    }
  }

  /**
   * Clean up old notifications (older than 10 days).
   * This method should be called by a scheduled task.
   */
  @Transactional
  public int cleanupOldNotifications() {
    Instant cutoffDate = Instant.now().minus(java.time.Duration.ofDays(10));
    
    // Find old notifications using repository query
    List<Notification> oldNotifications = notificationRepository.findAll().stream()
            .filter(n -> n.getCreatedAt() != null && n.getCreatedAt().isBefore(cutoffDate))
            .toList();
    
    if (oldNotifications.isEmpty()) {
      logger.debug("No old notifications to clean up");
      return 0;
    }
    
    int deletedCount = 0;
    for (Notification notification : oldNotifications) {
      try {
        notificationRepository.delete(notification);
        deletedCount++;
      } catch (Exception e) {
        logger.warn("Failed to delete notification {}: {}", notification.getId(), e.getMessage());
      }
    }
    
    logger.info("Cleaned up {} old notifications (older than 10 days)", deletedCount);
    return deletedCount;
  }

  /**
   * Clean up old deduplication log entries (older than 1 hour).
   * This method should be called by a scheduled task.
   */
  @Transactional
  public int cleanupDeduplicationLog() {
    Instant cutoffTime = Instant.now().minus(java.time.Duration.ofHours(1));
    int deletedCount = deduplicationLogRepository.deleteOlderThan(cutoffTime);
    logger.info("Cleaned up {} old deduplication log entries (older than 1 hour)", deletedCount);
    return deletedCount;
  }
}

