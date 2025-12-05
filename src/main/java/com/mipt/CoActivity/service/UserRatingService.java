package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.UserRatingRequest;
import com.mipt.CoActivity.dto.UserRatingSummaryResponse;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.ForbiddenException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.model.UserRating;
import com.mipt.CoActivity.repository.UserRatingRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class UserRatingService {
    private static final Logger logger = LoggerFactory.getLogger(UserRatingService.class);

    private final UserRatingRepository userRatingRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserRatingService(UserRatingRepository userRatingRepository, UserRepository userRepository) {
        this.userRatingRepository = userRatingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserRating createOrUpdateRating(Long ratedUserId, Long raterUserId, UserRatingRequest request) {
        if (ratedUserId.equals(raterUserId)) {
            throw new BadRequestException("Users cannot rate themselves");
        }

        User ratedUser = userRepository.findById(ratedUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Rated user not found"));
        
        User rater = userRepository.findById(raterUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Rater not found"));

        // Validate score
        if (request.getScore().compareTo(BigDecimal.ZERO) < 0 || 
            request.getScore().compareTo(BigDecimal.TEN) > 0) {
            throw new BadRequestException("Score must be between 0 and 10");
        }

        // Round to 1 decimal place
        BigDecimal score = request.getScore().setScale(1, RoundingMode.HALF_UP);

        Optional<UserRating> existingRating = userRatingRepository.findByRatedUserIdAndRaterUserId(ratedUserId, raterUserId);
        
        if (existingRating.isPresent()) {
            // Update existing rating
            UserRating rating = existingRating.get();
            rating.setScore(score);
            logger.info("Updated rating: user {} rated user {} with score {}", raterUserId, ratedUserId, score);
            return userRatingRepository.save(rating);
        } else {
            // Create new rating
            UserRating rating = new UserRating();
            rating.setRatedUser(ratedUser);
            rating.setRater(rater);
            rating.setScore(score);
            logger.info("Created rating: user {} rated user {} with score {}", raterUserId, ratedUserId, score);
            return userRatingRepository.save(rating);
        }
    }

    public UserRatingSummaryResponse getRatingSummary(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        Double average = userRatingRepository.findAverageRatingByRatedUserId(userId);
        Long count = userRatingRepository.countByRatedUserId(userId);

        // Round average to 1 decimal place
        Double roundedAverage = average != null 
            ? BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).doubleValue()
            : null;

        return new UserRatingSummaryResponse(roundedAverage, count != null ? count : 0L);
    }
}

