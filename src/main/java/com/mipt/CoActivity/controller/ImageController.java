package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/images")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class ImageController {
  private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
  private final ImageService imageService;

  @Autowired
  public ImageController(ImageService imageService) {
    this.imageService = imageService;
  }

  @PostMapping("/upload")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
    try {
      logger.info("Received image upload request. File name: {}, Size: {} bytes, Content type: {}", 
          file.getOriginalFilename(), file.getSize(), file.getContentType());
      
      Image image = imageService.uploadImage(file);
      logger.info("Image uploaded successfully with ID: {}", image.getId());
      return ResponseEntity.status(HttpStatus.CREATED).body(image);
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid image upload request: {}", e.getMessage());
      Map<String, String> error = new HashMap<>();
      error.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    } catch (IOException e) {
      logger.error("IO error while uploading image: {}", e.getMessage(), e);
      Map<String, String> error = new HashMap<>();
      error.put("error", "Failed to process image file: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    } catch (Exception e) {
      logger.error("Unexpected error while uploading image: {}", e.getMessage(), e);
      Map<String, String> error = new HashMap<>();
      error.put("error", "Unexpected error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<byte[]> getImage(@PathVariable Integer id) {
    try {
      Image image = imageService.getImage(id);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.IMAGE_JPEG); // Default to JPEG, can be enhanced to detect actual type
      return ResponseEntity.ok().headers(headers).body(image.getContent());
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }
}

