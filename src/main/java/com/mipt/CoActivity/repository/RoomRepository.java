package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
  Optional<Room> findByName(String name);
  List<Room> findByCollaboratorsId(Long userId);
  List<Room> findByAdminsId(Long userId);
}
