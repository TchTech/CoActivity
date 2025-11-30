package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/images")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class ImageController {
  private final ImageService imageService;

  @Autowired
  public ImageController(ImageService imageService) {
    this.imageService = imageService;
  }

  @PostMapping("/upload")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Image> uploadImage(@RequestParam("file") MultipartFile file) {
    try {
      Image image = imageService.uploadImage(file);
      return ResponseEntity.status(HttpStatus.CREATED).body(image);
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
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

