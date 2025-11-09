package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {}
