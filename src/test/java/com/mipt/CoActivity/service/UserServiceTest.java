package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomFolderRepository roomFolderRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ExternalLinkRepository externalLinkRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private InterestCategoryRepository interestCategoryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("user1", "user1@test.com", "password");
        user1.setId(1L);
        user1.setName("User 1");
        user1.setSubscriptions(new ArrayList<>());
        user1.setFollowers(new ArrayList<>());

        user2 = new User("user2", "user2@test.com", "password");
        user2.setId(2L);
        user2.setName("User 2");
        user2.setSubscriptions(new ArrayList<>());
        user2.setFollowers(new ArrayList<>());
    }

    @Test
    void testSubscribe_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.subscribe(1L, 2L);

        // Then
        verify(userRepository, times(2)).save(any(User.class));
        verify(notificationService, times(1)).createNotification(
            eq(2L),
            eq("FOLLOW"),
            eq("Новая подписка"),
            anyString(),
            anyString()
        );
    }

    @Test
    void testSubscribe_PreventsSelfSubscription() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userService.subscribe(1L, 1L);
        });

        assertEquals("Cannot subscribe to yourself", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never()).createNotification(
            anyLong(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    void testSubscribe_AlreadySubscribed_ReturnsSuccess() {
        // Given
        user1.getSubscriptions().add(user2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        userService.subscribe(1L, 2L);

        // Then
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testSubscribe_ThrowsExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.subscribe(1L, 2L);
        });
    }

    @Test
    void testUnsubscribe_Success() {
        // Given
        user1.getSubscriptions().add(user2);
        user2.getFollowers().add(user1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.unsubscribe(1L, 2L);

        // Then
        verify(userRepository, times(2)).save(any(User.class));
        assertFalse(user1.getSubscriptions().contains(user2));
        assertFalse(user2.getFollowers().contains(user1));
    }

    @Test
    void testUnsubscribe_NotSubscribed_ReturnsSuccess() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        userService.unsubscribe(1L, 2L);

        // Then
        verify(userRepository, never()).save(any(User.class));
    }
}

