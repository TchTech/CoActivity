package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.ApproveJoinRequestRequest;
import com.mipt.CoActivity.dto.MembershipRequestRequest;
import com.mipt.CoActivity.dto.RejectJoinRequestRequest;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.ConflictException;
import com.mipt.CoActivity.exception.ForbiddenException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.RoomJoinRequest;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.RoomJoinRequestHistoryRepository;
import com.mipt.CoActivity.repository.RoomJoinRequestRepository;
import com.mipt.CoActivity.repository.RoomRepository;
import com.mipt.CoActivity.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomJoinRequestServiceTest {

    @Mock
    private RoomJoinRequestRepository roomJoinRequestRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RoomJoinRequestHistoryRepository roomJoinRequestHistoryRepository;

    @InjectMocks
    private RoomJoinRequestService roomJoinRequestService;

    private Room testRoom;
    private User roomCreator;
    private User admin;
    private User applicant;
    private RoomJoinRequest pendingRequest;

    @BeforeEach
    void setUp() {
        roomCreator = new User("creator", "creator@test.com", "password");
        roomCreator.setId(1L);
        roomCreator.setName("Room Creator");

        admin = new User("admin", "admin@test.com", "password");
        admin.setId(2L);
        admin.setName("Admin User");

        applicant = new User("applicant", "applicant@test.com", "password");
        applicant.setId(3L);
        applicant.setName("Applicant User");

        testRoom = new Room(roomCreator, "Test Room");
        testRoom.setId(1L);
        testRoom.setJoinType("by_application");
        testRoom.setMaxCollaborators(5);
        testRoom.setIsClosed(false);
        testRoom.setCollaborators(new ArrayList<>(List.of(roomCreator)));
        testRoom.setAdmins(new ArrayList<>(List.of(roomCreator, admin)));

        pendingRequest = new RoomJoinRequest(testRoom, applicant);
        pendingRequest.setId(1L);
        pendingRequest.setStatus("pending");
        pendingRequest.setCreatedAt(Instant.now());
    }

    // ========== createRequest Tests ==========

    @Test
    void createRequest_Success() {
        // Arrange
        MembershipRequestRequest request = new MembershipRequestRequest();
        request.setMessage("I want to join this room");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.empty());
        when(roomJoinRequestRepository.findMostRecentRejection(3L, 1L))
                .thenReturn(Optional.empty());
        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class))).thenReturn(pendingRequest);

        // Act
        RoomJoinRequest result = roomJoinRequestService.createRequest(1L, 3L, request);

        // Assert
        assertNotNull(result);
        assertEquals("pending", result.getStatus());
        // Message is set from request if provided, otherwise null
        if (request != null && request.getMessage() != null) {
            assertEquals(request.getMessage(), result.getMessage());
        }
        verify(roomJoinRequestRepository, times(1)).save(any(RoomJoinRequest.class));
        verify(notificationService, atLeastOnce()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createRequest_RejectsOpenRoom() {
        // Arrange
        testRoom.setJoinType("open");
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            roomJoinRequestService.createRequest(1L, 3L, null);
        });

        assertTrue(exception.getMessage().contains("by_application"));
        verify(roomJoinRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_RejectsWhenUserAlreadyMember() {
        // Arrange
        testRoom.getCollaborators().add(applicant);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            roomJoinRequestService.createRequest(1L, 3L, null);
        });

        assertEquals("User is already a member of this room", exception.getMessage());
        verify(roomJoinRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_RejectsWhenPendingRequestExists() {
        // Arrange
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.of(pendingRequest));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            roomJoinRequestService.createRequest(1L, 3L, null);
        });

        assertEquals("You have already applied to this room", exception.getMessage());
        verify(roomJoinRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_RejectsDuringCooldown() {
        // Arrange
        RoomJoinRequest rejectedRequest = new RoomJoinRequest(testRoom, applicant);
        rejectedRequest.setStatus("rejected");
        rejectedRequest.setLastRejectedAt(Instant.now().minusSeconds(60)); // 1 minute ago (less than 5 minutes)

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.empty());
        when(roomJoinRequestRepository.findMostRecentRejection(3L, 1L))
                .thenReturn(Optional.of(rejectedRequest));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            roomJoinRequestService.createRequest(1L, 3L, null);
        });

        assertTrue(exception.getMessage().contains("wait"));
        verify(roomJoinRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_AllowsAfterCooldown() {
        // Arrange
        RoomJoinRequest rejectedRequest = new RoomJoinRequest(testRoom, applicant);
        rejectedRequest.setStatus("rejected");
        rejectedRequest.setLastRejectedAt(Instant.now().minusSeconds(360)); // 6 minutes ago (more than 5 minutes)

        MembershipRequestRequest request = new MembershipRequestRequest();
        request.setMessage("Reapplying after cooldown");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.empty());
        when(roomJoinRequestRepository.findMostRecentRejection(3L, 1L))
                .thenReturn(Optional.of(rejectedRequest));
        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class))).thenReturn(pendingRequest);

        // Act
        RoomJoinRequest result = roomJoinRequestService.createRequest(1L, 3L, request);

        // Assert
        assertNotNull(result);
        verify(roomJoinRequestRepository, times(1)).save(any(RoomJoinRequest.class));
    }

    @Test
    void createRequest_AllowsForClosedRoom() {
        // Arrange
        testRoom.setIsClosed(true);
        MembershipRequestRequest request = new MembershipRequestRequest();
        request.setMessage("Requesting closed room");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.empty());
        when(roomJoinRequestRepository.findMostRecentRejection(3L, 1L))
                .thenReturn(Optional.empty());
        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class))).thenReturn(pendingRequest);

        // Act
        RoomJoinRequest result = roomJoinRequestService.createRequest(1L, 3L, request);

        // Assert
        assertNotNull(result);
        // Should allow request even for closed room (per requirements)
        verify(roomJoinRequestRepository, times(1)).save(any(RoomJoinRequest.class));
    }

    // ========== approveRequest Tests ==========

    @Test
    void approveRequest_Success() {
        // Arrange
        ApproveJoinRequestRequest approveRequest = new ApproveJoinRequestRequest();
        approveRequest.setAdminId(2L); // admin

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.of(pendingRequest));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class))).thenReturn(pendingRequest);

        // Act
        roomJoinRequestService.approveRequest(1L, 1L, 3L, approveRequest);

        // Assert
        verify(roomJoinRequestRepository, times(1)).save(any(RoomJoinRequest.class));
        verify(roomRepository, times(1)).save(any(Room.class));
        assertTrue(testRoom.getCollaborators().contains(applicant));
        verify(notificationService, times(1)).createNotification(eq(3L), eq("MEMBERSHIP_APPROVED"), any(), any(), any(), eq(1L));
    }

    @Test
    void approveRequest_RejectsWhenNotAdmin() {
        // Arrange
        User nonAdmin = new User("nonadmin", "nonadmin@test.com", "password");
        nonAdmin.setId(4L);

        ApproveJoinRequestRequest approveRequest = new ApproveJoinRequestRequest();
        approveRequest.setAdminId(4L);

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(4L)).thenReturn(Optional.of(nonAdmin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            roomJoinRequestService.approveRequest(1L, 1L, 3L, approveRequest);
        });

        assertTrue(exception.getMessage().contains("creator or administrators"));
        verify(roomJoinRequestRepository, never()).save(any());
    }

    @Test
    void approveRequest_RejectsWhenRoomAtCapacity() {
        // Arrange
        testRoom.setMaxCollaborators(2);
        testRoom.getCollaborators().add(admin); // Now at capacity (creator + admin = 2)

        ApproveJoinRequestRequest approveRequest = new ApproveJoinRequestRequest();
        approveRequest.setAdminId(2L);

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.of(pendingRequest));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            roomJoinRequestService.approveRequest(1L, 1L, 3L, approveRequest);
        });

        assertEquals("Room has reached maximum capacity", exception.getMessage());
        verify(roomJoinRequestRepository, never()).save(any());
    }

    @Test
    void approveRequest_AutoRejectsOthersWhenCapacityReached() {
        // Arrange
        testRoom.setMaxCollaborators(2); // Only 1 slot left (creator already in)
        
        User applicant2 = new User("applicant2", "applicant2@test.com", "password");
        applicant2.setId(4L);
        RoomJoinRequest pendingRequest2 = new RoomJoinRequest(testRoom, applicant2);
        pendingRequest2.setId(2L);
        pendingRequest2.setStatus("pending");

        ApproveJoinRequestRequest approveRequest = new ApproveJoinRequestRequest();
        approveRequest.setAdminId(2L);

        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom)); // For autoRejectPendingRequests
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.of(pendingRequest));
        when(roomJoinRequestRepository.findPendingRequestsByRoom(1L))
                .thenReturn(List.of(pendingRequest2)); // Other pending requests
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class))).thenReturn(pendingRequest);

        // Act
        roomJoinRequestService.approveRequest(1L, 1L, 3L, approveRequest);

        // Assert
        verify(roomJoinRequestRepository, atLeast(2)).save(any(RoomJoinRequest.class)); // Approve + auto-reject others
    }

    // ========== rejectRequest Tests ==========

    @Test
    void rejectRequest_Success() {
        // Arrange
        RejectJoinRequestRequest rejectRequest = new RejectJoinRequestRequest();
        rejectRequest.setAdminId(2L);
        rejectRequest.setReason("Not suitable for this room");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(applicant));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 3L, "pending"))
                .thenReturn(Optional.of(pendingRequest));
        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class))).thenReturn(pendingRequest);

        // Act
        roomJoinRequestService.rejectRequest(1L, 1L, 3L, rejectRequest);

        // Assert
        verify(roomJoinRequestRepository, times(1)).save(any(RoomJoinRequest.class));
        assertEquals("rejected", pendingRequest.getStatus());
        assertNotNull(pendingRequest.getLastRejectedAt());
        assertEquals("Not suitable for this room", pendingRequest.getRejectionReason());
        verify(notificationService, times(1)).createNotification(eq(3L), eq("MEMBERSHIP_REJECTED"), any(), any(), any(), eq(1L));
    }

    // ========== cancelRequest Tests ==========

    @Test
    void cancelRequest_Success() {
        // Arrange
        when(roomJoinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class))).thenReturn(pendingRequest);

        // Act
        roomJoinRequestService.cancelRequest(1L, 1L, 3L);

        // Assert
        verify(roomJoinRequestRepository, times(1)).save(any(RoomJoinRequest.class));
        assertEquals("cancelled", pendingRequest.getStatus());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelRequest_RejectsWhenNotOwner() {
        // Arrange
        when(roomJoinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            roomJoinRequestService.cancelRequest(1L, 1L, 999L); // Different user
        });

        assertTrue(exception.getMessage().contains("own requests"));
        verify(roomJoinRequestRepository, never()).save(any());
    }

    @Test
    void cancelRequest_RejectsWhenNotPending() {
        // Arrange
        pendingRequest.setStatus("approved");
        when(roomJoinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            roomJoinRequestService.cancelRequest(1L, 1L, 3L);
        });

        assertTrue(exception.getMessage().contains("pending"));
        verify(roomJoinRequestRepository, never()).save(any());
    }
}

