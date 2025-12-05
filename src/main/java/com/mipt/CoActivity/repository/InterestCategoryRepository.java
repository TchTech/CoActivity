package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.InterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterestCategoryRepository extends JpaRepository<InterestCategory, Integer> {
  Optional<InterestCategory> findByName(String name);
}

