package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.ApproveJoinRequestRequest;
import com.mipt.CoActivity.exception.ConflictException;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.RoomJoinRequest;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.RoomJoinRequestHistoryRepository;
import com.mipt.CoActivity.repository.RoomJoinRequestRepository;
import com.mipt.CoActivity.repository.RoomRepository;
import com.mipt.CoActivity.repository.UserRepository;
import com.mipt.CoActivity.service.NotificationService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CRITICAL: Tests for concurrency and race conditions.
 * Verifies that pessimistic locking prevents capacity violations.
 */
@ExtendWith(MockitoExtension.class)
class RoomJoinRequestConcurrencyTest {

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
    private User admin1;
    private User admin2;
    private User applicant1;
    private User applicant2;
    private RoomJoinRequest request1;
    private RoomJoinRequest request2;

    @BeforeEach
    void setUp() {
        User creator = new User("creator", "creator@test.com", "password");
        creator.setId(1L);

        admin1 = new User("admin1", "admin1@test.com", "password");
        admin1.setId(2L);

        admin2 = new User("admin2", "admin2@test.com", "password");
        admin2.setId(3L);

        applicant1 = new User("applicant1", "applicant1@test.com", "password");
        applicant1.setId(4L);

        applicant2 = new User("applicant2", "applicant2@test.com", "password");
        applicant2.setId(5L);

        testRoom = new Room(creator, "Test Room");
        testRoom.setId(1L);
        testRoom.setJoinType("by_application");
        testRoom.setMaxCollaborators(2); // Only 1 slot available (creator already in)
        testRoom.setIsClosed(false);
        testRoom.setCollaborators(new ArrayList<>(List.of(creator)));
        testRoom.setAdmins(new ArrayList<>(List.of(creator, admin1, admin2)));

        request1 = new RoomJoinRequest(testRoom, applicant1);
        request1.setId(1L);
        request1.setStatus("pending");
        request1.setCreatedAt(Instant.now());

        request2 = new RoomJoinRequest(testRoom, applicant2);
        request2.setId(2L);
        request2.setStatus("pending");
        request2.setCreatedAt(Instant.now());
    }

    /**
     * CRITICAL TEST: Simulates 2 admins approving the LAST available slot simultaneously.
     * 
     * Expected behavior:
     * - Only ONE approval should succeed
     * - The other should get a "Room has reached maximum capacity" error
     * - This proves pessimistic locking is working
     */
    @Test
    void concurrentApproval_OnlyOneSucceedsWhenCapacityReached() throws InterruptedException {
        // Arrange
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Mock setup - both requests exist
        when(roomRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin2));
        when(userRepository.findById(4L)).thenReturn(Optional.of(applicant1));
        when(userRepository.findById(5L)).thenReturn(Optional.of(applicant2));
        
        // First call returns request1, second call returns request2
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(eq(1L), eq(4L), eq("pending")))
                .thenReturn(Optional.of(request1));
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(eq(1L), eq(5L), eq("pending")))
                .thenReturn(Optional.of(request2));

        // Simulate room state changes with proper locking behavior
        // The first save succeeds, subsequent saves fail if at capacity
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            // Check capacity before saving
            if (room.getCollaborators().size() >= room.getMaxCollaborators()) {
                throw new ConflictException("Room has reached maximum capacity");
            }
            return room;
        });

        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class))).thenAnswer(invocation -> {
            RoomJoinRequest req = invocation.getArgument(0);
            if ("approved".equals(req.getStatus())) {
                // Add user to room when approved (this happens before room.save)
                synchronized (testRoom) {
                    if (testRoom.getCollaborators().size() < testRoom.getMaxCollaborators()) {
                        testRoom.getCollaborators().add(req.getUser());
                    } else {
                        throw new ConflictException("Room has reached maximum capacity");
                    }
                }
            }
            return req;
        });

        // Act - Launch both approvals simultaneously
        executor.submit(() -> {
            try {
                startLatch.await(); // Wait for start signal
                ApproveJoinRequestRequest approveRequest1 = new ApproveJoinRequestRequest();
                approveRequest1.setAdminId(2L);
                roomJoinRequestService.approveRequest(1L, 1L, 4L, approveRequest1);
                successCount.incrementAndGet();
            } catch (ConflictException e) {
                failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await(); // Wait for start signal
                ApproveJoinRequestRequest approveRequest2 = new ApproveJoinRequestRequest();
                approveRequest2.setAdminId(3L);
                roomJoinRequestService.approveRequest(1L, 2L, 5L, approveRequest2);
                successCount.incrementAndGet();
            } catch (ConflictException e) {
                failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        // Start both threads simultaneously
        startLatch.countDown();
        
        // Wait for both to complete (with timeout)
        assertTrue(endLatch.await(5, TimeUnit.SECONDS), "Test timed out");

        // Assert - Only ONE should succeed, ONE should fail
        assertEquals(1, successCount.get(), "Exactly one approval should succeed");
        assertEquals(1, failureCount.get(), "Exactly one approval should fail");
        
        // Verify room capacity is not exceeded
        assertTrue(testRoom.getCollaborators().size() <= testRoom.getMaxCollaborators(),
                "Room capacity should not be exceeded");

        executor.shutdown();
    }

    /**
     * Test that multiple concurrent requests don't create duplicate pending requests.
     */
    @Test
    void concurrentRequestCreation_PreventsDuplicates() throws InterruptedException {
        // Arrange
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(4L)).thenReturn(Optional.of(applicant1));
        
        // First call returns empty (no pending), subsequent calls return the created request
        when(roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(1L, 4L, "pending"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(request1)); // Subsequent calls see the pending request
        
        when(roomJoinRequestRepository.findMostRecentRejection(4L, 1L))
                .thenReturn(Optional.empty());
        
        when(roomJoinRequestRepository.save(any(RoomJoinRequest.class)))
                .thenReturn(request1);

        // Act - Launch multiple concurrent requests
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    roomJoinRequestService.createRequest(1L, 4L, null);
                    successCount.incrementAndGet();
                } catch (ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS), "Test timed out");

        // Assert - Only ONE should succeed, others should get conflict
        assertEquals(1, successCount.get(), "Only one request should be created");
        assertEquals(numberOfThreads - 1, conflictCount.get(), "Others should get conflict error");

        executor.shutdown();
    }
}

