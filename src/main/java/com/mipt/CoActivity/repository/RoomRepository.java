package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
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
  
  // Найти комнаты, события которых прошли в указанный день (от начала до конца дня)
  @Query("SELECT r FROM Room r WHERE r.meetingTime >= :startOfDay AND r.meetingTime < :endOfDay AND r.meetingTime IS NOT NULL")
  List<Room> findRoomsWithMeetingTimeBetween(@Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);
}
