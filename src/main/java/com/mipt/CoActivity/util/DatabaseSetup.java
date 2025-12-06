package com.mipt.CoActivity.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Утилита для автоматического создания пользователя и базы данных PostgreSQL
 * Пробует подключиться как postgres с разными паролями
 */
public class DatabaseSetup {
    
    private static final String HOST = "localhost";
    private static final int PORT = 5432;
    private static final String DEFAULT_DB = "postgres";
    private static final String POSTGRES_USER = "postgres";
    private static final List<String> POSSIBLE_PASSWORDS = Arrays.asList("", "postgres", "admin", "root");
    
    private static final String NEW_USER = "Gr1zBear";
    private static final String NEW_PASSWORD = "qwerty";
    private static final String NEW_DATABASE = "CoPos";
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Автоматическое создание БД PostgreSQL");
        System.out.println("========================================");
        System.out.println();
        
        Connection postgresConn = null;
        String workingPassword = null;
        
        // Пробуем подключиться как postgres с разными паролями
        for (String password : POSSIBLE_PASSWORDS) {
            String url = String.format("jdbc:postgresql://%s:%d/%s", HOST, PORT, DEFAULT_DB);
            System.out.println("Попытка подключения с паролем: " + 
                (password.isEmpty() ? "(пустой)" : "***"));
            
            try {
                postgresConn = DriverManager.getConnection(url, POSTGRES_USER, password);
                System.out.println("✓ Успешно подключились!");
                workingPassword = password;
                break;
            } catch (Exception e) {
                System.out.println("✗ Не удалось: " + e.getMessage());
            }
        }
        
        if (postgresConn == null) {
            System.out.println();
            System.out.println("ОШИБКА: Не удалось подключиться к PostgreSQL как postgres");
            System.out.println("Пожалуйста, создайте пользователя и БД вручную через pgAdmin");
            System.out.println();
            System.out.println("SQL команды:");
            System.out.println("CREATE USER \"" + NEW_USER + "\" WITH PASSWORD '" + NEW_PASSWORD + "';");
            System.out.println("CREATE DATABASE \"" + NEW_DATABASE + "\" OWNER \"" + NEW_USER + "\";");
            System.out.println("GRANT ALL PRIVILEGES ON DATABASE \"" + NEW_DATABASE + "\" TO \"" + NEW_USER + "\";");
            return;
        }
        
        try {
            Statement stmt = postgresConn.createStatement();
            
            // Удаляем пользователя, если существует
            System.out.println();
            System.out.println("Удаление старого пользователя (если существует)...");
            try {
                stmt.execute("DROP USER IF EXISTS \"" + NEW_USER + "\";");
                System.out.println("✓ Старый пользователь удален");
            } catch (Exception e) {
                System.out.println("  (пользователь не существовал)");
            }
            
            // Создаем пользователя
            System.out.println("Создание пользователя " + NEW_USER + "...");
            stmt.execute("CREATE USER \"" + NEW_USER + "\" WITH PASSWORD '" + NEW_PASSWORD + "';");
            System.out.println("✓ Пользователь создан!");
            
            // Удаляем БД, если существует
            System.out.println();
            System.out.println("Удаление старой БД (если существует)...");
            try {
                stmt.execute("DROP DATABASE IF EXISTS \"" + NEW_DATABASE + "\";");
                System.out.println("✓ Старая БД удалена");
            } catch (Exception e) {
                System.out.println("  (БД не существовала)");
            }
            
            // Создаем БД
            System.out.println("Создание базы данных " + NEW_DATABASE + "...");
            stmt.execute("CREATE DATABASE \"" + NEW_DATABASE + "\" OWNER \"" + NEW_USER + "\";");
            System.out.println("✓ База данных создана!");
            
            // Выдаем привилегии
            System.out.println();
            System.out.println("Выдача привилегий...");
            stmt.execute("GRANT ALL PRIVILEGES ON DATABASE \"" + NEW_DATABASE + "\" TO \"" + NEW_USER + "\";");
            System.out.println("✓ Привилегии выданы!");
            
            // Проверка подключения с новым пользователем
            System.out.println();
            System.out.println("Проверка подключения с новым пользователем...");
            String newUrl = String.format("jdbc:postgresql://%s:%d/%s", HOST, PORT, NEW_DATABASE);
            try (Connection testConn = DriverManager.getConnection(newUrl, NEW_USER, NEW_PASSWORD)) {
                System.out.println("✓ Подключение с пользователем " + NEW_USER + " успешно!");
            }
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("Готово! Пользователь и БД созданы.");
            System.out.println("Теперь можно запускать Spring Boot приложение.");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("ОШИБКА при создании пользователя/БД: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (postgresConn != null) {
                    postgresConn.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }
}

