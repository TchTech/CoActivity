package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.NotificationDeduplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface NotificationDeduplicationLogRepository extends JpaRepository<NotificationDeduplicationLog, Long> {

    Optional<NotificationDeduplicationLog> findByDeduplicationHashAndUser_IdAndNotificationType(
            String deduplicationHash, Long userId, String notificationType);


    @Modifying
    @Query("DELETE FROM NotificationDeduplicationLog n WHERE n.createdAt < :timestamp")
    int deleteOlderThan(@Param("timestamp") Instant timestamp);
}

