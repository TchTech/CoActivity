package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Notification;
import com.mipt.CoActivity.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class NotificationController {
  private final NotificationService notificationService;

  @Autowired
  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
    return ResponseEntity.ok(notificationService.getUserNotifications(userId));
  }

  @GetMapping("/{userId}/unread")
  public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
    return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
  }

  @GetMapping("/{userId}/unread-count")
  public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
    return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
  }

  @PostMapping("/{notificationId}/read")
  public ResponseEntity<Void> markAsRead(
      @PathVariable Integer notificationId, @RequestParam Long userId) {
    notificationService.markAsRead(notificationId.longValue(), userId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{userId}/read-all")
  public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
    notificationService.markAllAsRead(userId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/mark-read")
  public ResponseEntity<Void> markNotificationsAsRead(
      @RequestParam Long userId, @RequestBody List<Integer> notificationIds) {
    notificationService.markNotificationsAsRead(notificationIds, userId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> dismissNotification(
      @PathVariable Integer notificationId, @RequestParam Long userId) {
    notificationService.dismissNotification(notificationId, userId);
    return ResponseEntity.ok().build();
  }
}

