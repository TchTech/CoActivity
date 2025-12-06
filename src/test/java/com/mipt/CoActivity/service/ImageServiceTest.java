package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.repository.ImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private ImageService imageService;

    private Image testImage;
    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        testImage = new Image();
        testImage.setId(1);
        testImage.setContent(new byte[]{1, 2, 3});
        
        testFile = mock(MultipartFile.class);
        when(testFile.getOriginalFilename()).thenReturn("test.jpg");
        when(testFile.getContentType()).thenReturn("image/jpeg");
    }

    @Test
    void testGetImage_Success() {
        // Given
        when(imageRepository.findById(1)).thenReturn(Optional.of(testImage));

        // When
        Image result = imageService.getImage(1);

        // Then
        assertNotNull(result);
        assertEquals(testImage.getId(), result.getId());
    }

    @Test
    void testGetImage_ThrowsExceptionWhenNotFound() {
        // Given
        when(imageRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            imageService.getImage(999);
        });
    }
}

