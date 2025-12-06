package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ImageController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageService imageService;

    private Image testImage;
    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() {
        testImage = new Image();
        testImage.setId(1);
        testImage.setContent(new byte[]{1, 2, 3, 4, 5});

        testFile = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3, 4, 5}
        );
    }

    @Test
    void testUploadImage_Success() throws Exception {
        // Given
        when(imageService.uploadImage(any())).thenReturn(testImage);

        // When & Then
        mockMvc.perform(multipart("/images/upload")
                .file(testFile))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testUploadImage_ReturnsBadRequestOnInvalidFile() throws Exception {
        // Given
        when(imageService.uploadImage(any())).thenThrow(new IllegalArgumentException("File cannot be empty"));

        // When & Then
        mockMvc.perform(multipart("/images/upload")
                .file(testFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testUploadImage_ReturnsInternalServerErrorOnIOException() throws Exception {
        // Given
        when(imageService.uploadImage(any())).thenThrow(new IOException("IO error"));

        // When & Then
        mockMvc.perform(multipart("/images/upload")
                .file(testFile))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testGetImage_Success() throws Exception {
        // Given
        when(imageService.getImage(1)).thenReturn(testImage);

        // When & Then
        mockMvc.perform(get("/images/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void testGetImage_ReturnsNotFound() throws Exception {
        // Given
        when(imageService.getImage(999)).thenThrow(new RuntimeException("Image not found"));

        // When & Then
        mockMvc.perform(get("/images/999"))
                .andExpect(status().isNotFound());
    }
}

