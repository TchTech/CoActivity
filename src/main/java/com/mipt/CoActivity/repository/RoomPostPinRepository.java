package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.RoomPostPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomPostPinRepository extends JpaRepository<RoomPostPin, Long> {
    List<RoomPostPin> findByRoomId(Long roomId);
    List<RoomPostPin> findByPostId(Integer postId);
    Optional<RoomPostPin> findByRoomIdAndPostId(Long roomId, Integer postId);
    void deleteByRoomIdAndPostId(Long roomId, Integer postId);
}

