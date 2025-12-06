package com.mipt.CoActivity.command;

import com.mipt.CoActivity.util.DatabaseCleanupUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Команда для очистки базы данных.
 * Использование: запустите приложение с параметром --clear-database=true
 */
@Component
@ConditionalOnProperty(name = "app.clear-database", havingValue = "true")
public class ClearDatabaseCommand implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ClearDatabaseCommand.class);
    
    private final DatabaseCleanupUtil databaseCleanupUtil;
    
    @Autowired
    public ClearDatabaseCommand(DatabaseCleanupUtil databaseCleanupUtil) {
        this.databaseCleanupUtil = databaseCleanupUtil;
    }
    
    @Override
    public void run(String... args) {
        logger.warn("========================================");
        logger.warn("ВЫПОЛНЯЕТСЯ ОЧИСТКА БАЗЫ ДАННЫХ!");
        logger.warn("========================================");
        try {
            databaseCleanupUtil.clearAllTables();
            logger.warn("========================================");
            logger.warn("Очистка завершена. Приложение будет остановлено.");
            logger.warn("========================================");
            System.exit(0);
        } catch (Exception e) {
            logger.error("Ошибка при очистке базы данных: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}

