package com.mipt.CoActivity.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseCleanupUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupUtil.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Очищает все таблицы в базе данных.
     * ВНИМАНИЕ: Этот метод удалит ВСЕ данные!
     */
    @Transactional
    public void clearAllTables() {
        logger.warn("Начинается очистка базы данных...");
        
        try {
            // Отключаем проверку внешних ключей
            entityManager.createNativeQuery("SET session_replication_role = 'replica'").executeUpdate();
            
            // Очищаем таблицы в правильном порядке
            String[] tables = {
                "notification_deduplication_log",
                "notifications",
                "room_join_request_history",
                "room_join_requests",
                "room_post_pin",
                "messages",
                "user_ratings",
                "\"likesOnPosts\"",
                "\"dislikesOnPosts\"",
                "\"likesOnComments\"",
                "\"dislikesOnComments\"",
                "comments",
                "posts",
                "user_subscriptions",
                "user_interests",
                "room_admins",
                "\"roomsCollaborators\"",
                "room_notification_settings",
                "user_settings",
                "feedbacks",
                "external_links",
                "interests",
                "interest_categories",
                "rooms",
                "images",
                "email_verification_tokens",
                "password_reset_tokens",
                "users"
            };
            
            for (String table : tables) {
                try {
                    entityManager.createNativeQuery("TRUNCATE TABLE " + table + " CASCADE").executeUpdate();
                    logger.debug("Очищена таблица: {}", table);
                } catch (Exception e) {
                    logger.warn("Не удалось очистить таблицу {}: {}", table, e.getMessage());
                }
            }
            
            // Включаем обратно проверку внешних ключей
            entityManager.createNativeQuery("SET session_replication_role = 'origin'").executeUpdate();
            
            // Сбрасываем последовательности
            String[] sequences = {
                "users_id_seq",
                "rooms_id_seq",
                "posts_id_seq",
                "comments_id_seq",
                "messages_id_seq",
                "images_id_seq",
                "user_ratings_id_seq",
                "room_join_requests_id_seq",
                "notifications_id_seq",
                "interests_id_seq",
                "interest_categories_id_seq",
                "external_links_id_seq",
                "password_reset_tokens_id_seq",
                "email_verification_tokens_id_seq"
            };
            
            for (String seq : sequences) {
                try {
                    entityManager.createNativeQuery("ALTER SEQUENCE IF EXISTS " + seq + " RESTART WITH 1").executeUpdate();
                    logger.debug("Сброшена последовательность: {}", seq);
                } catch (Exception e) {
                    logger.warn("Не удалось сбросить последовательность {}: {}", seq, e.getMessage());
                }
            }
            
            logger.info("База данных успешно очищена!");
            
        } catch (Exception e) {
            logger.error("Ошибка при очистке базы данных: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось очистить базу данных", e);
        }
    }
}

