package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}