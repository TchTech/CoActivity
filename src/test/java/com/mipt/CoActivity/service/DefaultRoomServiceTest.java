package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.RoomRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DefaultRoomService defaultRoomService;

    private User testUser;
    private Room defaultRoom;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);

        defaultRoom = new Room(testUser, "General");
        defaultRoom.setId(1L);
        defaultRoom.setDescription("Default room for all users");
        defaultRoom.setCategory("General");
        defaultRoom.setIsDefault(true);
        defaultRoom.setJoinType("open");
        defaultRoom.setCollaborators(new ArrayList<>());
        defaultRoom.setAdmins(new ArrayList<>());
    }

    @Test
    void testRun_CreatesDefaultRoomWhenNotExists() {
        // Given
        when(roomRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(roomRepository.save(any(Room.class))).thenReturn(defaultRoom);

        // When
        defaultRoomService.run();

        // Then
        verify(roomRepository, times(2)).save(any(Room.class)); // Once for creation, once for adding members
    }

    @Test
    void testRun_DoesNotCreateWhenNoUsers() {
        // Given
        when(roomRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        // When
        defaultRoomService.run();

        // Then
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void testRun_UpdatesDefaultRoomWhenExists() {
        // Given
        User newUser = new User("newuser", "new@example.com", "password");
        newUser.setId(2L);
        defaultRoom.getCollaborators().add(testUser);
        when(roomRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultRoom));
        when(userRepository.findAll()).thenReturn(List.of(testUser, newUser));
        when(roomRepository.save(any(Room.class))).thenReturn(defaultRoom);

        // When
        defaultRoomService.run();

        // Then
        verify(roomRepository, times(1)).save(defaultRoom);
    }

    @Test
    void testRun_DoesNotUpdateWhenAllUsersAlreadyMembers() {
        // Given
        defaultRoom.getCollaborators().add(testUser);
        when(roomRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultRoom));
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        // When
        defaultRoomService.run();

        // Then
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void testInitializeDefaultRoom_CreatesRoom() {
        // Given
        when(roomRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(roomRepository.save(any(Room.class))).thenReturn(defaultRoom);

        // When
        defaultRoomService.initializeDefaultRoom();

        // Then
        verify(roomRepository, times(2)).save(any(Room.class));
    }
}

