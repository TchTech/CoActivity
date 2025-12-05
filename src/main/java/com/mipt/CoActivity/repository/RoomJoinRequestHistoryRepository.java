package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.RoomJoinRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomJoinRequestHistoryRepository extends JpaRepository<RoomJoinRequestHistory, Long> {
    List<RoomJoinRequestHistory> findByRequest_IdOrderByChangedAtAsc(Long requestId);
    List<RoomJoinRequestHistory> findByChangedBy_Id(Long userId);
}

