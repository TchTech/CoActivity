package com.mipt.CoActivity.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Standalone утилита для очистки базы данных.
 * Можно запустить напрямую без Spring Boot контекста.
 * 
 * Использование:
 * java -cp "target/classes:target/dependency/*" com.mipt.CoActivity.util.StandaloneDatabaseCleaner
 */
public class StandaloneDatabaseCleaner {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/CoPos";
    private static final String DB_USER = "Gr1zBear";
    private static final String DB_PASSWORD = "qwerty";
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Очистка базы данных CoPos");
        System.out.println("========================================");
        System.out.println("");
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Подключено к базе данных.");
            
            // Отключаем проверку внешних ключей (требует права суперпользователя)
            // Если нет прав, CASCADE в TRUNCATE справится сам
            try {
                stmt.execute("SET session_replication_role = 'replica'");
            } catch (Exception e) {
                System.out.println("Предупреждение: нет прав для изменения session_replication_role, продолжаем без этого...");
            }
            
            // Очищаем таблицы
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
                    stmt.execute("TRUNCATE TABLE " + table + " CASCADE");
                    System.out.println("Очищена таблица: " + table);
                } catch (Exception e) {
                    System.out.println("Предупреждение: не удалось очистить таблицу " + table + ": " + e.getMessage());
                }
            }
            
            // Включаем обратно проверку внешних ключей
            try {
                stmt.execute("SET session_replication_role = 'origin'");
            } catch (Exception e) {
                // Игнорируем ошибку, если нет прав
            }
            
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
                    stmt.execute("ALTER SEQUENCE IF EXISTS " + seq + " RESTART WITH 1");
                    System.out.println("Сброшена последовательность: " + seq);
                } catch (Exception e) {
                    System.out.println("Предупреждение: не удалось сбросить последовательность " + seq + ": " + e.getMessage());
                }
            }
            
            System.out.println("");
            System.out.println("========================================");
            System.out.println("База данных успешно очищена!");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("ОШИБКА при очистке базы данных:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

