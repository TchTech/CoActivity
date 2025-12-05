package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
  List<Comment> findByPostId(Integer postId);
  
  List<Comment> findByPostIdAndParentCommentIsNull(Integer postId);
  
  List<Comment> findByParentCommentId(Long parentCommentId);
  
  @Modifying
  @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
  void deleteByPostId(@Param("postId") Integer postId);
}