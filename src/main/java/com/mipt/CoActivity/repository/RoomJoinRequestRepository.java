package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.RoomJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomJoinRequestRepository extends JpaRepository<RoomJoinRequest, Long> {
  List<RoomJoinRequest> findByRoomIdAndStatus(Long roomId, String status);
  List<RoomJoinRequest> findByRoomId(Long roomId);
  List<RoomJoinRequest> findByRoomCreatedByIdAndStatus(Long creatorId, String status);
  Optional<RoomJoinRequest> findByRoomIdAndUserIdAndStatus(Long roomId, Long userId, String status);
  List<RoomJoinRequest> findByUserIdAndStatus(Long userId, String status);
  
  /**
   * Find the most recent rejected request for a user and room.
   * Used to check if cooldown period (5 minutes) has passed.
   * 
   * @param userId The user ID
   * @param roomId The room ID
   * @return Optional containing the most recent rejected request, if any
   */
  @Query("SELECT r FROM RoomJoinRequest r " +
         "WHERE r.user.id = :userId " +
         "AND r.room.id = :roomId " +
         "AND r.status = 'rejected' " +
         "AND r.lastRejectedAt IS NOT NULL " +
         "ORDER BY r.lastRejectedAt DESC")
  List<RoomJoinRequest> findRejectedRequestsByUserAndRoom(@Param("userId") Long userId, @Param("roomId") Long roomId);
  
  /**
   * Find the most recent rejected request for a user and room (convenience method).
   * Returns the first result from findRejectedRequestsByUserAndRoom.
   */
  default Optional<RoomJoinRequest> findMostRecentRejection(Long userId, Long roomId) {
    List<RoomJoinRequest> rejected = findRejectedRequestsByUserAndRoom(userId, roomId);
    return rejected.isEmpty() ? Optional.empty() : Optional.of(rejected.get(0));
  }
  
  /**
   * Find all pending requests for a room, ordered by creation time (oldest first).
   * Used for auto-rejecting requests when room closes or reaches capacity.
   * 
   * @param roomId The room ID
   * @return List of pending requests
   */
  @Query("SELECT r FROM RoomJoinRequest r " +
         "WHERE r.room.id = :roomId " +
         "AND r.status = 'pending' " +
         "ORDER BY r.createdAt ASC")
  List<RoomJoinRequest> findPendingRequestsByRoom(@Param("roomId") Long roomId);
}

