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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
     * Запускается каждый день в 00:01
     * Cron expression: "0 1 0 * * ?" означает: секунда=0, минута=1, час=0, каждый день
     */
    @Scheduled(cron = "0 1 0 * * ?")
    @Transactional
    public void sendRatingNotificationsForYesterdayRooms() {
        logger.info("[RoomRatingNotificationScheduler] Starting scheduled task to send rating notifications");
        
        try {
            // Получаем вчерашний день (начало и конец дня)
            // Используем UTC для консистентности
            LocalDate yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
            LocalDateTime startOfYesterday = yesterday.atStartOfDay(); // 00:00:00 вчера UTC
            LocalDateTime endOfYesterday = yesterday.atTime(23, 59, 59, 999999999); // 23:59:59.999999999 вчера UTC
            
            // Конвертируем в Instant (UTC)
            Instant startOfDay = startOfYesterday.atZone(ZoneId.of("UTC")).toInstant();
            Instant endOfDay = endOfYesterday.atZone(ZoneId.of("UTC")).toInstant();
            
            logger.info("[RoomRatingNotificationScheduler] Looking for rooms with meetingTime between {} and {}", 
                    startOfDay, endOfDay);
            
            // Находим все комнаты, события которых прошли вчера
            List<Room> roomsWithYesterdayEvents = roomRepository.findRoomsWithMeetingTimeBetween(startOfDay, endOfDay);
            
            logger.info("[RoomRatingNotificationScheduler] Found {} rooms with events yesterday", 
                    roomsWithYesterdayEvents.size());
            
            int totalNotificationsSent = 0;
            
            // Для каждой комнаты отправляем уведомления всем участникам
            for (Room room : roomsWithYesterdayEvents) {
                if (room.getCollaborators() == null || room.getCollaborators().isEmpty()) {
                    logger.debug("[RoomRatingNotificationScheduler] Room {} has no collaborators, skipping", room.getId());
                    continue;
                }
                
                logger.info("[RoomRatingNotificationScheduler] Processing room {}: '{}' with {} collaborators", 
                        room.getId(), room.getName(), room.getCollaborators().size());
                
                // Отправляем уведомление каждому участнику
                for (User participant : room.getCollaborators()) {
                    try {
                        // Формируем текст уведомления
                        String title = "Оцените участников комнаты";
                        String content = String.format(
                                "Событие комнаты '%s' завершилось. Пожалуйста, оцените других участников.",
                                room.getName() != null ? room.getName() : "Комната"
                        );
                        
                        // В data сохраняем информацию о комнате для возможного использования на фронтенде
                        String data = String.format(
                                "{\"roomId\":%d,\"roomName\":\"%s\"}",
                                room.getId(),
                                room.getName() != null ? room.getName().replace("\"", "\\\"") : "Комната"
                        );
                        
                        // Создаем уведомление
                        notificationService.createNotification(
                                participant.getId(),
                                "ROOM_RATING_REMINDER",
                                title,
                                content,
                                data
                        );
                        
                        totalNotificationsSent++;
                        logger.debug("[RoomRatingNotificationScheduler] Sent rating notification to user {} for room {}", 
                                participant.getId(), room.getId());
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

