package com.mipt.CoActivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service for cleaning up old notifications and deduplication logs.
 * 
 * Scheduled tasks:
 * - Cleanup old notifications (every day at 2 AM)
 * - Cleanup old deduplication logs (every hour)
 */
@Service
public class NotificationCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationCleanupService.class);

    private final NotificationService notificationService;
    private final RoomService roomService;

    @Autowired
    public NotificationCleanupService(NotificationService notificationService, RoomService roomService) {
        this.notificationService = notificationService;
        this.roomService = roomService;
    }

    /**
     * Clean up old notifications (older than 10 days).
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
    @Transactional
    public void cleanupOldNotifications() {
        logger.info("Starting scheduled cleanup of old notifications...");
        try {
            int deletedCount = notificationService.cleanupOldNotifications();
            logger.info("Scheduled cleanup completed: deleted {} old notifications", deletedCount);
        } catch (Exception e) {
            logger.error("Error during scheduled notification cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old deduplication log entries (older than 1 hour).
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour at minute 0
    @Transactional
    public void cleanupDeduplicationLog() {
        logger.debug("Starting scheduled cleanup of deduplication log...");
        try {
            int deletedCount = notificationService.cleanupDeduplicationLog();
            logger.debug("Scheduled deduplication log cleanup completed: deleted {} entries", deletedCount);
        } catch (Exception e) {
            logger.error("Error during scheduled deduplication log cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Auto-close expired rooms (where meetingTime has passed).
     * Runs every hour at minute 15.
     */
    @Scheduled(cron = "0 15 * * * ?") // Every hour at minute 15
    @Transactional
    public void autoCloseExpiredRooms() {
        logger.info("Starting scheduled auto-close of expired rooms...");
        try {
            roomService.autoCloseExpiredRooms();
            logger.info("Scheduled auto-close of expired rooms completed");
        } catch (Exception e) {
            logger.error("Error during scheduled room auto-close: {}", e.getMessage(), e);
        }
    }
}

