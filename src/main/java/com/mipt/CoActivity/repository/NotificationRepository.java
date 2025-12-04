package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
  List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
  long countByUserIdAndIsReadFalse(Long userId);
  Optional<Notification> findById(long notificationId);
}

