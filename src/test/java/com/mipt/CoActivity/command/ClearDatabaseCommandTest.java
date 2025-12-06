package com.mipt.CoActivity.command;

import com.mipt.CoActivity.util.DatabaseCleanupUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearDatabaseCommandTest {

    @Mock
    private DatabaseCleanupUtil databaseCleanupUtil;

    private ClearDatabaseCommand clearDatabaseCommand;

    @BeforeEach
    void setUp() {
        clearDatabaseCommand = new ClearDatabaseCommand(databaseCleanupUtil);
        doNothing().when(databaseCleanupUtil).clearAllTables();
    }

    @Test
    void testRun_Success() {
        // When
        try {
            clearDatabaseCommand.run();
        } catch (SecurityException e) {
            // System.exit(0) will throw SecurityException in tests, which is expected
        }

        // Then
        verify(databaseCleanupUtil, times(1)).clearAllTables();
    }

    @Test
    void testRun_WithException() {
        // Given
        doThrow(new RuntimeException("Database error")).when(databaseCleanupUtil).clearAllTables();

        // When & Then
        try {
            clearDatabaseCommand.run();
        } catch (SecurityException e) {
            // System.exit(1) will throw SecurityException in tests, which is expected
        } catch (RuntimeException e) {
            // Expected from databaseCleanupUtil
        }
        verify(databaseCleanupUtil, times(1)).clearAllTables();
    }
}

