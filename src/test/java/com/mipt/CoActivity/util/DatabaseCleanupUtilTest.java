package com.mipt.CoActivity.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseCleanupUtilTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private DatabaseCleanupUtil databaseCleanupUtil;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleanupUtil = new DatabaseCleanupUtil();
        // Inject EntityManager using reflection
        Field entityManagerField = DatabaseCleanupUtil.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        entityManagerField.set(databaseCleanupUtil, entityManager);
        
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
    }

    @Test
    void testClearAllTables_Success() {
        // When
        databaseCleanupUtil.clearAllTables();

        // Then
        verify(entityManager, atLeastOnce()).createNativeQuery(anyString());
        verify(query, atLeastOnce()).executeUpdate();
    }

    @Test
    void testClearAllTables_WithException() {
        // Given
        when(query.executeUpdate()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            databaseCleanupUtil.clearAllTables();
        } catch (RuntimeException e) {
            // Expected
        }
        verify(entityManager, atLeastOnce()).createNativeQuery(anyString());
    }
}

