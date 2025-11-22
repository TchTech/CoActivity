package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.RoomFolder;
import com.mipt.CoActivity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomFolderRepository extends JpaRepository<RoomFolder, Long> {
    List<RoomFolder> findByUser(User user);
    List<RoomFolder> findByUserId(Long userId);
}

