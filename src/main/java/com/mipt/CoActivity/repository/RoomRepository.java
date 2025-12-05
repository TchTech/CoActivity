package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
  Optional<Room> findByName(String name);
  List<Room> findByCollaboratorsId(Long userId);
  List<Room> findByAdminsId(Long userId);
  List<Room> findByNameContainingIgnoreCase(String name);
  List<Room> findByDescriptionContainingIgnoreCase(String description);
  java.util.Optional<Room> findByIsDefaultTrue();
  
  /**
   * Find room by ID with pessimistic write lock for concurrency control.
   * Used when checking and updating room capacity to prevent race conditions.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM Room r WHERE r.id = :roomId")
  Optional<Room> findByIdWithLock(@Param("roomId") Long roomId);
}
