package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.UserRatingRequest;
import com.mipt.CoActivity.dto.UserRatingSummaryResponse;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.model.UserRating;
import com.mipt.CoActivity.repository.UserRatingRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRatingServiceTest {

    @Mock
    private UserRatingRepository userRatingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRatingService userRatingService;

    private User ratedUser;
    private User rater;
    private UserRatingRequest ratingRequest;
    private UserRating existingRating;

    @BeforeEach
    void setUp() {
        ratedUser = new User("rateduser", "rated@example.com", "password");
        ratedUser.setId(1L);

        rater = new User("rater", "rater@example.com", "password");
        rater.setId(2L);

        ratingRequest = new UserRatingRequest();
        ratingRequest.setScore(BigDecimal.valueOf(8.5));
        // UserRatingRequest may not have setReview() - check the actual structure

        existingRating = new UserRating();
        existingRating.setId(1L);
        existingRating.setRatedUser(ratedUser);
        existingRating.setRater(rater);
        existingRating.setScore(BigDecimal.valueOf(7.0));
    }

    @Test
    void testCreateOrUpdateRating_CreatesNewRating() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(ratedUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(rater));
        when(userRatingRepository.findByRatedUserIdAndRaterUserId(1L, 2L)).thenReturn(Optional.empty());
        when(userRatingRepository.save(any(UserRating.class))).thenReturn(existingRating);

        // When
        UserRating result = userRatingService.createOrUpdateRating(1L, 2L, ratingRequest);

        // Then
        assertNotNull(result);
        verify(userRatingRepository, times(1)).save(any(UserRating.class));
    }

    @Test
    void testCreateOrUpdateRating_UpdatesExistingRating() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(ratedUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(rater));
        when(userRatingRepository.findByRatedUserIdAndRaterUserId(1L, 2L)).thenReturn(Optional.of(existingRating));
        when(userRatingRepository.save(any(UserRating.class))).thenReturn(existingRating);

        // When
        UserRating result = userRatingService.createOrUpdateRating(1L, 2L, ratingRequest);

        // Then
        assertNotNull(result);
        verify(userRatingRepository, times(1)).save(existingRating);
    }

    @Test
    void testCreateOrUpdateRating_ThrowsExceptionWhenSelfRating() {
        // When & Then
        assertThrows(BadRequestException.class, () -> {
            userRatingService.createOrUpdateRating(1L, 1L, ratingRequest);
        });
    }

    @Test
    void testCreateOrUpdateRating_ThrowsExceptionWhenRatedUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userRatingService.createOrUpdateRating(999L, 2L, ratingRequest);
        });
    }

    @Test
    void testCreateOrUpdateRating_ThrowsExceptionWhenScoreTooHigh() {
        // Given
        ratingRequest.setScore(BigDecimal.valueOf(11.0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(ratedUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(rater));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            userRatingService.createOrUpdateRating(1L, 2L, ratingRequest);
        });
    }

    @Test
    void testCreateOrUpdateRating_ThrowsExceptionWhenScoreTooLow() {
        // Given
        ratingRequest.setScore(BigDecimal.valueOf(-1.0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(ratedUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(rater));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            userRatingService.createOrUpdateRating(1L, 2L, ratingRequest);
        });
    }

    @Test
    void testGetRatingSummary_Success() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRatingRepository.findAverageRatingByRatedUserId(1L)).thenReturn(8.5);
        when(userRatingRepository.countByRatedUserId(1L)).thenReturn(10L);

        // When
        UserRatingSummaryResponse result = userRatingService.getRatingSummary(1L);

        // Then
        assertNotNull(result);
        assertEquals(8.5, result.getAverage());
        assertEquals(10L, result.getCount());
    }

    @Test
    void testGetRatingSummary_ThrowsExceptionWhenUserNotFound() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userRatingService.getRatingSummary(999L);
        });
    }

    @Test
    void testGetRatingSummary_HandlesNullAverage() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRatingRepository.findAverageRatingByRatedUserId(1L)).thenReturn(null);
        when(userRatingRepository.countByRatedUserId(1L)).thenReturn(0L);

        // When
        UserRatingSummaryResponse result = userRatingService.getRatingSummary(1L);

        // Then
        assertNotNull(result);
        assertNull(result.getAverage());
        assertEquals(0L, result.getCount());
    }
}

