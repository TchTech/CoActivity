package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.util.DatabaseCleanupUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AdminController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseCleanupUtil databaseCleanupUtil;

    @Test
    void testClearDatabase_Success() throws Exception {
        // Given
        doNothing().when(databaseCleanupUtil).clearAllTables();

        // When & Then
        mockMvc.perform(post("/admin/clear-database")
                .param("confirm", "yes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testClearDatabase_ReturnsBadRequestWithoutConfirmation() throws Exception {
        // When & Then
        mockMvc.perform(post("/admin/clear-database"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testClearDatabase_ReturnsBadRequestWithWrongConfirmation() throws Exception {
        // When & Then
        mockMvc.perform(post("/admin/clear-database")
                .param("confirm", "no"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testClearDatabase_HandlesException() throws Exception {
        // Given
        doThrow(new RuntimeException("Database error")).when(databaseCleanupUtil).clearAllTables();

        // When & Then
        mockMvc.perform(post("/admin/clear-database")
                .param("confirm", "yes"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }
}

