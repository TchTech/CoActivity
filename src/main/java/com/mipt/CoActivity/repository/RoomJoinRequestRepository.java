package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.RoomJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomJoinRequestRepository extends JpaRepository<RoomJoinRequest, Long> {
  List<RoomJoinRequest> findByRoomIdAndStatus(Long roomId, String status);
  List<RoomJoinRequest> findByRoomId(Long roomId);
  List<RoomJoinRequest> findByRoomCreatedByIdAndStatus(Long creatorId, String status);
  Optional<RoomJoinRequest> findByRoomIdAndUserIdAndStatus(Long roomId, Long userId, String status);
  List<RoomJoinRequest> findByUserIdAndStatus(Long userId, String status);
}

