package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled task для отправки уведомлений об оценке участников комнаты
 * Запускается каждый день в 00:01 и отправляет уведомления участникам комнат,
 * события которых прошли вчера
 */
@Service
public class RoomRatingNotificationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(RoomRatingNotificationScheduler.class);
    
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;
    
    @Autowired
    public RoomRatingNotificationScheduler(
            RoomRepository roomRepository,
            NotificationService notificationService) {
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }
    
    /**
     * Запускается каждый час
     * Проверяет комнаты, у которых endTime наступил, и отправляет уведомления об оценке создателя
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void sendRatingNotificationsForEndedRooms() {
        logger.info("[RoomRatingNotificationScheduler] Starting scheduled task to send rating notifications for ended rooms");
        
        try {
            Instant now = Instant.now();
            // Находим комнаты, у которых endTime наступил в последний час
            Instant oneHourAgo = now.minus(1, java.time.temporal.ChronoUnit.HOURS);
            
            logger.info("[RoomRatingNotificationScheduler] Looking for rooms with endTime between {} and {}", 
                    oneHourAgo, now);
            
            // Находим все комнаты, у которых endTime наступил в последний час
            List<Room> endedRooms = roomRepository.findAll().stream()
                .filter(room -> room.getEndTime() != null)
                .filter(room -> !room.getEndTime().isAfter(now))
                .filter(room -> room.getEndTime().isAfter(oneHourAgo))
                .toList();
            
            logger.info("[RoomRatingNotificationScheduler] Found {} rooms that ended", endedRooms.size());
            
            int totalNotificationsSent = 0;
            
            // Для каждой комнаты отправляем уведомления всем участникам об оценке создателя
            for (Room room : endedRooms) {
                if (room.getCreatedBy() == null) {
                    logger.debug("[RoomRatingNotificationScheduler] Room {} has no creator, skipping", room.getId());
                    continue;
                }
                
                if (room.getCollaborators() == null || room.getCollaborators().isEmpty()) {
                    logger.debug("[RoomRatingNotificationScheduler] Room {} has no collaborators, skipping", room.getId());
                    continue;
                }
                
                logger.info("[RoomRatingNotificationScheduler] Processing room {}: '{}' with {} collaborators", 
                        room.getId(), room.getName(), room.getCollaborators().size());
                
                Long creatorId = room.getCreatedBy().getId();
                String creatorName = room.getCreatedBy().getName() != null 
                    ? room.getCreatedBy().getName() 
                    : room.getCreatedBy().getUsername();
                
                // Отправляем уведомление каждому участнику (кроме создателя) об оценке создателя
                for (User participant : room.getCollaborators()) {
                    // Пропускаем создателя
                    if (participant.getId().equals(creatorId)) {
                        continue;
                    }
                    
                    try {
                        // Формируем текст уведомления
                        String title = "Оцените создателя комнаты";
                        String content = String.format(
                                "Комната '%s' завершила свое существование. Пожалуйста, оцените создателя комнаты %s.",
                                room.getName() != null ? room.getName() : "Комната",
                                creatorName
                        );
                        
                        // В data сохраняем информацию о комнате и создателе
                        String data = String.format(
                                "{\"roomId\":%d,\"roomName\":\"%s\",\"creatorId\":%d,\"creatorName\":\"%s\",\"type\":\"rateCreator\"}",
                                room.getId(),
                                room.getName() != null ? room.getName().replace("\"", "\\\"") : "Комната",
                                creatorId,
                                creatorName.replace("\"", "\\\"")
                        );
                        
                        // Создаем уведомление
                        notificationService.createNotification(
                                participant.getId(),
                                "RATE_ROOM_CREATOR",
                                title,
                                content,
                                data
                        );
                        
                        totalNotificationsSent++;
                        logger.debug("[RoomRatingNotificationScheduler] Sent rating notification to user {} for room creator {}", 
                                participant.getId(), creatorId);
                    } catch (Exception e) {
                        logger.error("[RoomRatingNotificationScheduler] Error sending notification to user {} for room {}: {}", 
                                participant.getId(), room.getId(), e.getMessage(), e);
                    }
                }
            }
            
            logger.info("[RoomRatingNotificationScheduler] Completed. Sent {} notifications total", totalNotificationsSent);
        } catch (Exception e) {
            logger.error("[RoomRatingNotificationScheduler] Error in scheduled task: {}", e.getMessage(), e);
        }
    }
}

