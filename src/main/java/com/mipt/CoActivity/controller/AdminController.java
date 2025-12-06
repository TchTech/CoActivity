package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.util.DatabaseCleanupUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final DatabaseCleanupUtil databaseCleanupUtil;
    
    @Autowired
    public AdminController(DatabaseCleanupUtil databaseCleanupUtil) {
        this.databaseCleanupUtil = databaseCleanupUtil;
    }
    
    /**
     * Очищает все данные из базы данных.
     * ВНИМАНИЕ: Этот эндпоинт удалит ВСЕ данные!
     * 
     * @param confirm Подтверждение (должно быть "yes")
     * @return Результат операции
     */
    @PostMapping("/clear-database")
    public ResponseEntity<Map<String, String>> clearDatabase(@RequestParam(required = false, defaultValue = "") String confirm) {
        if (!"yes".equalsIgnoreCase(confirm)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Требуется подтверждение: ?confirm=yes"));
        }
        
        try {
            logger.warn("Запрошена очистка базы данных через API");
            databaseCleanupUtil.clearAllTables();
            return ResponseEntity.ok(Map.of("message", "База данных успешно очищена"));
        } catch (Exception e) {
            logger.error("Ошибка при очистке базы данных: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Не удалось очистить базу данных: " + e.getMessage()));
        }
    }
}

