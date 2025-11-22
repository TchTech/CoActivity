package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.RoomNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomNotificationSettingsRepository extends JpaRepository<RoomNotificationSettings, Long> {
    Optional<RoomNotificationSettings> findByRoom(Room room);
    Optional<RoomNotificationSettings> findByRoomId(Long roomId);
}

