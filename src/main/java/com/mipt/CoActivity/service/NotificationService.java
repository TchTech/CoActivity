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

  @Transactional
  public Notification createNotification(Long userId, String type, String title, String content, String data) {
    com.mipt.CoActivity.model.User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    Notification notification = new Notification();
    notification.setUser(user);
    notification.setType(type);
    notification.setTitle(title);
    notification.setContent(content);
    notification.setData(data);
    notification.setIsRead(false);
    notification.setCreatedAt(java.time.Instant.now());
    
    return notificationRepository.save(notification);
  }

  @Transactional
  public void markNotificationsAsRead(List<Integer> notificationIds, Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    for (Integer notificationId : notificationIds) {
      Notification notification = notificationRepository.findById(notificationId)
          .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
      
      if (!notification.getUser().getId().equals(userId)) {
        throw new com.mipt.CoActivity.exception.ForbiddenException("You don't have permission to modify this notification");
      }
      
      notification.setIsRead(true);
      notificationRepository.save(notification);
    }
  }

  @Transactional
  public void dismissNotification(Integer notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    
    if (!notification.getUser().getId().equals(userId)) {
      throw new com.mipt.CoActivity.exception.ForbiddenException("You don't have permission to dismiss this notification");
    }
    
    notificationRepository.delete(notification);
  }
}

