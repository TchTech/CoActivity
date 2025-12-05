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
  List<Room> findByLocationContainingIgnoreCase(String location);
  java.util.Optional<Room> findByIsDefaultTrue();
  
  /**
   * Find rooms by category (exact match, case-insensitive)
   */
  List<Room> findByCategoryIgnoreCase(String category);
  
  /**
   * Find rooms by category and start date
   */
  @Query("SELECT r FROM Room r WHERE LOWER(r.category) = LOWER(:category) AND r.meetingTime >= :startDate")
  List<Room> findByCategoryAndStartDate(
      @Param("category") String category,
      @Param("startDate") Instant startDate
  );
  
  /**
   * Find rooms by category and end date
   */
  @Query("SELECT r FROM Room r WHERE LOWER(r.category) = LOWER(:category) AND (r.endTime <= :endDate OR (r.endTime IS NULL AND r.meetingTime <= :endDate))")
  List<Room> findByCategoryAndEndDate(
      @Param("category") String category,
      @Param("endDate") Instant endDate
  );
  
  /**
   * Find rooms by category, start date and end date
   */
  @Query("SELECT r FROM Room r WHERE LOWER(r.category) = LOWER(:category) AND r.meetingTime >= :startDate AND (r.endTime <= :endDate OR (r.endTime IS NULL AND r.meetingTime <= :endDate))")
  List<Room> findByCategoryAndDateRange(
      @Param("category") String category,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate
  );
  
  /**
   * Find rooms by start date only
   */
  @Query("SELECT r FROM Room r WHERE r.meetingTime >= :startDate")
  List<Room> findByStartDate(@Param("startDate") Instant startDate);
  
  /**
   * Find rooms by end date only
   */
  @Query("SELECT r FROM Room r WHERE r.endTime <= :endDate OR (r.endTime IS NULL AND r.meetingTime <= :endDate)")
  List<Room> findByEndDate(@Param("endDate") Instant endDate);
  
  /**
   * Find rooms by start date and end date
   */
  @Query("SELECT r FROM Room r WHERE r.meetingTime >= :startDate AND (r.endTime <= :endDate OR (r.endTime IS NULL AND r.meetingTime <= :endDate))")
  List<Room> findByDateRange(
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate
  );
  
  /**
   * Find room by ID with pessimistic write lock for concurrency control.
   * Used when checking and updating room capacity to prevent race conditions.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM Room r WHERE r.id = :roomId")
  Optional<Room> findByIdWithLock(@Param("roomId") Long roomId);
}
