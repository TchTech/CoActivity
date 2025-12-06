package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
  List<Post> findByAuthorUsername(String username);
  
  @Query("SELECT p FROM Post p WHERE p.author.id IN :authorIds")
  List<Post> findByAuthorIdIn(@Param("authorIds") List<Long> authorIds);
  
  List<Post> findByAuthor(User author);
}
