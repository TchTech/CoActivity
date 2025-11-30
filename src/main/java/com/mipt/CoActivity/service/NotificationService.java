package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Notification;
import com.mipt.CoActivity.repository.NotificationRepository;
import com.mipt.CoActivity.repository.UserRepository;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  @Autowired
  public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
  }

  public List<Notification> getUserNotifications(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  public List<Notification> getUnreadNotifications(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
  }

  public long getUnreadCount(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  @Transactional
  public void markAsRead(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    
    if (!notification.getUser().getId().equals(userId)) {
      throw new com.mipt.CoActivity.exception.ForbiddenException("You don't have permission to modify this notification");
    }
    
    notification.setIsRead(true);
    notificationRepository.save(notification);
  }

  @Transactional
  public void markAllAsRead(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    for (Notification notification : unreadNotifications) {
      notification.setIsRead(true);
      notificationRepository.save(notification);
    }
  }
}

