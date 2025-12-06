package com.mipt.CoActivity.service;

import com.mipt.CoActivity.exception.ForbiddenException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import com.mipt.CoActivity.repository.MessageRepository;
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
import static org.mockito.Mockito.lenient;

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

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private RoomNotificationSettingsRepository roomNotificationSettingsRepository;

    @Mock
    private com.mipt.CoActivity.repository.RoomJoinRequestRepository roomJoinRequestRepository;

    @Mock
    private com.mipt.CoActivity.repository.RoomPostPinRepository roomPostPinRepository;

    @Mock
    private com.mipt.CoActivity.repository.PostRepository postRepository;

    @Mock
    private com.mipt.CoActivity.repository.ImageRepository imageRepository;

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
        room.getAdmins().add(member); // member is admin
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(notificationService.createNotification(any(), any(), any(), any(), any(), any())).thenReturn(new com.mipt.CoActivity.model.Notification());

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
        // admin (id=2) is not the creator (id=1), so should throw ForbiddenException

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            roomService.promoteToAdmin(1L, 3L, 2L);
        });
    }

    @Test
    void testDemoteFromAdmin_ThrowsExceptionWhenNotCreator() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        // admin is not the creator, so should throw ForbiddenException

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
        when(userRepository.findById(3L)).thenReturn(Optional.of(member));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            roomService.kickUserFromRoom(1L, 3L, 4L);
        });
    }

    @Test
    void testCreateRoom_Success() {
        // Given
        com.mipt.CoActivity.dto.CreateRoomRequest request = new com.mipt.CoActivity.dto.CreateRoomRequest();
        request.setDescription("Test Room Description");
        request.setCategory("Java");
        request.setMaxCollaborators(10);
        request.setMeetingTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));
        request.setMeetingType("online");
        request.setJoinType("open");

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        // When
        Room result = roomService.createRoom(1L, request);

        // Then
        assertNotNull(result);
        assertEquals("Test Room Description", result.getDescription());
        assertEquals("Java", result.getCategory());
        assertEquals(10, result.getMaxCollaborators());
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Test
    void testCreateRoom_ThrowsExceptionWhenUserNotFound() {
        // Given
        com.mipt.CoActivity.dto.CreateRoomRequest request = new com.mipt.CoActivity.dto.CreateRoomRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(com.mipt.CoActivity.exception.ResourceNotFoundException.class, () -> {
            roomService.createRoom(1L, request);
        });
    }

    @Test
    void testCreateRoom_ThrowsExceptionWhenMaxCollaboratorsInvalid() {
        // Given
        com.mipt.CoActivity.dto.CreateRoomRequest request = new com.mipt.CoActivity.dto.CreateRoomRequest();
        request.setMaxCollaborators(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        // When & Then
        assertThrows(com.mipt.CoActivity.exception.BadRequestException.class, () -> {
            roomService.createRoom(1L, request);
        });
    }

    @Test
    void testAddUserToRoom_Success() {
        // Given
        User newUser = new User("newuser", "newuser@test.com", "password");
        newUser.setId(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(newUser));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        roomService.addUserToRoom(4L, 1L);

        // Then
        assertTrue(room.getCollaborators().contains(newUser));
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void testAddUserToRoom_ThrowsExceptionWhenRoomFull() {
        // Given
        room.setMaxCollaborators(2);
        User newUser = new User("newuser", "newuser@test.com", "password");
        newUser.setId(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(newUser));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        // When & Then
        assertThrows(com.mipt.CoActivity.exception.ConflictException.class, () -> {
            roomService.addUserToRoom(4L, 1L);
        });
    }

    @Test
    void testRemoveUserFromRoom_Success() {
        // Given
        when(userRepository.findById(3L)).thenReturn(Optional.of(member));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        roomService.removeUserFromRoom(3L, 1L);

        // Then
        assertFalse(room.getCollaborators().contains(member));
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void testSearchRooms_WithQuery() {
        // Given
        List<Room> byName = new ArrayList<>();
        byName.add(room);
        List<Room> byDescription = new ArrayList<>();
        List<Room> byLocation = new ArrayList<>();

        when(roomRepository.findByNameContainingIgnoreCase("test")).thenReturn(byName);
        when(roomRepository.findByDescriptionContainingIgnoreCase("test")).thenReturn(byDescription);
        when(roomRepository.findByLocationContainingIgnoreCase("test")).thenReturn(byLocation);

        // When
        List<Room> result = roomService.searchRooms("test");

        // Then
        assertNotNull(result);
        assertTrue(result.contains(room));
    }

    @Test
    void testSearchRooms_WithoutQuery() {
        // Given
        List<Room> allRooms = new ArrayList<>();
        allRooms.add(room);
        when(roomRepository.findAll()).thenReturn(allRooms);

        // When
        List<Room> result = roomService.searchRooms(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testIsRoomClosed_AutoClosesWhenEventDatePassed() {
        // Given
        room.setIsClosed(false);
        room.setMeetingTime(Instant.now().minusSeconds(3600)); // 1 hour ago
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        boolean result = roomService.isRoomClosed(1L);

        // Then
        assertTrue(result);
        assertTrue(room.getIsClosed());
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void testRequestRating_Success() {
        // Given
        User participant = new User("participant", "participant@test.com", "password");
        participant.setId(4L);
        room.getCollaborators().add(participant);
        
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member));
        when(notificationService.createNotification(any(), any(), any(), any(), any(), any())).thenReturn(new com.mipt.CoActivity.model.Notification());

        // When
        roomService.requestRating(1L, 3L, 1L);

        // Then
        // requestRating sends notifications to all participants except requester and requested user
        // Since we added a participant, notification should be sent
        verify(notificationService, atLeastOnce()).createNotification(
            eq(4L),
            eq("RATE_USER_REQUEST"),
            anyString(),
            anyString(),
            any(),
            eq(1L)
        );
    }
}

