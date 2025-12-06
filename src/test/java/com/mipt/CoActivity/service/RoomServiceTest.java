package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.ForbiddenException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomJoinRequestService roomJoinRequestService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private User creator;
    private User admin;
    private User member;

    @BeforeEach
    void setUp() {
        creator = new User("creator", "creator@test.com", "password");
        creator.setId(1L);
        creator.setName("Creator");

        admin = new User("admin", "admin@test.com", "password");
        admin.setId(2L);
        admin.setName("Admin");

        member = new User("member", "member@test.com", "password");
        member.setId(3L);
        member.setName("Member");

        room = new Room();
        room.setId(1L);
        room.setName("Test Room");
        room.setCreatedBy(creator);
        room.setCollaborators(new ArrayList<>());
        room.setAdmins(new ArrayList<>());
        room.getCollaborators().add(creator);
        room.getCollaborators().add(member);
        room.getAdmins().add(admin);
        room.setIsClosed(false);
    }

    @Test
    void testCloseRoom_Success() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        roomService.closeRoom(1L, 1L);

        // Then
        assertTrue(room.getIsClosed());
        assertNotNull(room.getClosedAt());
        assertEquals(creator, room.getClosedBy());
        verify(roomJoinRequestService, times(1)).autoRejectPendingRequests(eq(1L), anyString(), anyString());
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void testCloseRoom_OnlyCreatorOrAdminCanClose() {
        // Given
        User unauthorizedUser = new User("unauthorized", "unauthorized@test.com", "password");
        unauthorizedUser.setId(4L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(4L)).thenReturn(Optional.of(unauthorizedUser));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            roomService.closeRoom(1L, 4L);
        });
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void testCloseRoom_ThrowsExceptionWhenRoomNotFound() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            roomService.closeRoom(1L, 1L);
        });
    }

    @Test
    void testPromoteToAdmin_Success() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        roomService.promoteToAdmin(1L, 3L, 1L);

        // Then
        assertTrue(room.getAdmins().contains(member));
        verify(notificationService, atLeastOnce()).createNotification(
            eq(3L),
            eq("ADMIN_PROMOTED"),
            anyString(),
            anyString(),
            any(),
            eq(1L)
        );
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void testDemoteFromAdmin_Success() {
        // Given
        room.getAdmins().add(member);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        roomService.demoteFromAdmin(1L, 3L, 1L);

        // Then
        assertFalse(room.getAdmins().contains(member));
        verify(notificationService, atLeastOnce()).createNotification(
            eq(3L),
            eq("ADMIN_DEMOTED"),
            anyString(),
            anyString(),
            any(),
            eq(1L)
        );
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void testKickUserFromRoom_Success() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        roomService.kickUserFromRoom(1L, 3L, 2L);

        // Then
        assertFalse(room.getCollaborators().contains(member));
        // Note: kickUserFromRoom doesn't send notifications in current implementation
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void testIsRoomClosed_ReturnsTrueWhenClosed() {
        // Given
        room.setIsClosed(true);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        // When
        boolean result = roomService.isRoomClosed(1L);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsRoomClosed_ReturnsFalseWhenOpen() {
        // Given
        room.setIsClosed(false);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        // When
        boolean result = roomService.isRoomClosed(1L);

        // Then
        assertFalse(result);
    }

    @Test
    void testCloseRoom_AdminCanClose() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        roomService.closeRoom(1L, 2L);

        // Then
        assertTrue(room.getIsClosed());
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void testPromoteToAdmin_ThrowsExceptionWhenNotCreator() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            roomService.promoteToAdmin(1L, 3L, 2L);
        });
    }

    @Test
    void testDemoteFromAdmin_ThrowsExceptionWhenNotCreator() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            roomService.demoteFromAdmin(1L, 3L, 2L);
        });
    }

    @Test
    void testKickUserFromRoom_ThrowsExceptionWhenNotAdmin() {
        // Given
        User unauthorizedUser = new User("unauthorized", "unauthorized@test.com", "password");
        unauthorizedUser.setId(4L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(4L)).thenReturn(Optional.of(unauthorizedUser));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            roomService.kickUserFromRoom(1L, 3L, 4L);
        });
    }
}

