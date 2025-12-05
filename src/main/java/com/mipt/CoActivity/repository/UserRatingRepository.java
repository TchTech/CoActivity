package com.mipt.CoActivity.repository;

import com.mipt.CoActivity.model.UserRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRating, Long> {
    @Query("SELECT ur FROM UserRating ur WHERE ur.ratedUser.id = :ratedUserId AND ur.rater.id = :raterUserId")
    Optional<UserRating> findByRatedUserIdAndRaterUserId(@Param("ratedUserId") Long ratedUserId, @Param("raterUserId") Long raterUserId);
    
    @Query("SELECT ur FROM UserRating ur WHERE ur.ratedUser.id = :ratedUserId")
    List<UserRating> findByRatedUserId(@Param("ratedUserId") Long ratedUserId);
    
    @Query("SELECT AVG(ur.score) FROM UserRating ur WHERE ur.ratedUser.id = :ratedUserId")
    Double findAverageRatingByRatedUserId(@Param("ratedUserId") Long ratedUserId);
    
    @Query("SELECT COUNT(ur) FROM UserRating ur WHERE ur.ratedUser.id = :ratedUserId")
    Long countByRatedUserId(@Param("ratedUserId") Long ratedUserId);
}

