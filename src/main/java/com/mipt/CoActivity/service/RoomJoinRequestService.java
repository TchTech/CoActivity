package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.ApproveJoinRequestRequest;
import com.mipt.CoActivity.dto.MembershipRequestRequest;
import com.mipt.CoActivity.dto.RejectJoinRequestRequest;
import com.mipt.CoActivity.exception.*;
import com.mipt.CoActivity.model.*;
import com.mipt.CoActivity.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RoomJoinRequestService {
    private static final Logger logger = LoggerFactory.getLogger(RoomJoinRequestService.class);
    private static final long COOLDOWN_MINUTES = 5;

    private final RoomJoinRequestRepository roomJoinRequestRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RoomJoinRequestHistoryRepository roomJoinRequestHistoryRepository;

    @Autowired
    public RoomJoinRequestService(
            RoomJoinRequestRepository roomJoinRequestRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            RoomJoinRequestHistoryRepository roomJoinRequestHistoryRepository) {
        this.roomJoinRequestRepository = roomJoinRequestRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.roomJoinRequestHistoryRepository = roomJoinRequestHistoryRepository;
    }

    /**
     * Create a new room join request with comprehensive validation.
     * 
     * Validations:
     * - Room must exist and have joinType = "by_application"
     * - Room must not be closed
     * - User must not already be a collaborator
     * - User must not have a pending request
     * - Cooldown check: 5 minutes must have passed since last rejection
     * 
     * @param roomId The room ID
     * @param userId The user ID requesting to join
     * @param request Optional request with message
     * @return The created RoomJoinRequest
     */
    @Transactional
    public RoomJoinRequest createRequest(Long roomId, Long userId, MembershipRequestRequest request) {
        // Fetch room with validation
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Validate room type - ONLY "by_application" rooms accept requests
        if (!"by_application".equals(room.getJoinType())) {
            throw new BadRequestException("Requests can only be created for rooms with joinType 'by_application'. This room is '" + room.getJoinType() + "'");
        }

        // Validate room is not closed
        if (Boolean.TRUE.equals(room.getIsClosed())) {
            // Per requirements: Allow request creation even if room is closed
            // Admins can manually approve it later
            logger.warn("User {} attempting to create request for closed room {}", userId, roomId);
        }

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate user is not already a collaborator
        if (room.getCollaborators().contains(user)) {
            throw new ConflictException("User is already a member of this room");
        }

        // Check for existing pending request
        Optional<RoomJoinRequest> existingPending = roomJoinRequestRepository
                .findByRoomIdAndUserIdAndStatus(roomId, userId, "pending");
        if (existingPending.isPresent()) {
            throw new ConflictException("You have already applied to this room");
        }

        // Check if user was already a collaborator but has a pending request (edge case)
        // This should not happen, but we handle it by auto-rejecting the pending request
        Optional<RoomJoinRequest> existingRequest = roomJoinRequestRepository
                .findByRoomIdAndUserIdAndStatus(roomId, userId, "pending");
        if (existingRequest.isPresent() && room.getCollaborators().contains(user)) {
            logger.warn("Found pending request for user {} who is already a collaborator in room {}. Auto-rejecting.", userId, roomId);
            RoomJoinRequest pendingRequest = existingRequest.get();
            pendingRequest.setStatus("rejected");
            pendingRequest.setRespondedAt(Instant.now());
            pendingRequest.setRejectionReason("User is already a member of this room");
            roomJoinRequestRepository.save(pendingRequest);
            throw new ConflictException("User is already a member of this room");
        }

        // Check cooldown: 5 minutes must have passed since last rejection
        Optional<RoomJoinRequest> mostRecentRejection = roomJoinRequestRepository
                .findMostRecentRejection(userId, roomId);
        if (mostRecentRejection.isPresent()) {
            RoomJoinRequest lastRejection = mostRecentRejection.get();
            if (lastRejection.getLastRejectedAt() != null) {
                Duration timeSinceRejection = Duration.between(lastRejection.getLastRejectedAt(), Instant.now());
                if (timeSinceRejection.toMinutes() < COOLDOWN_MINUTES) {
                    long remainingMinutes = COOLDOWN_MINUTES - timeSinceRejection.toMinutes();
                    throw new BadRequestException(
                            String.format("You must wait %d more minute(s) before reapplying to this room", remainingMinutes)
                    );
                }
            }
        }

        // Create the request
        RoomJoinRequest joinRequest = new RoomJoinRequest(room, user);
        if (request != null && request.getMessage() != null) {
            // Validate message length (max 500 characters per requirements)
            if (request.getMessage().length() > 500) {
                throw new BadRequestException("Message cannot exceed 500 characters");
            }
            joinRequest.setMessage(request.getMessage());
        }

        RoomJoinRequest savedRequest = roomJoinRequestRepository.save(joinRequest);

        // Create history entry
        createHistoryEntry(savedRequest, null, "pending", user, "Request created");

        // Send notifications to room creator and all admins
        sendNewRequestNotifications(room, savedRequest, user);

        logger.info("User {} created membership request {} for room {}", userId, savedRequest.getId(), roomId);
        return savedRequest;
    }

    /**
     * Approve a room join request with capacity checking and race condition prevention.
     * 
     * Validations:
     * - Admin must be room creator or admin
     * - Request must be pending
     * - User must not already be a collaborator
     * - Room must have capacity (with database locking to prevent race conditions)
     * 
     * Side effects:
     * - If approving causes room to reach capacity, auto-reject all other pending requests
     * 
     * @param roomId The room ID
     * @param requestId The request ID (optional, can use targetUserId instead)
     * @param targetUserId The user ID whose request is being approved
     * @param approveRequest The approval request with adminId
     */
    @Transactional
    public void approveRequest(Long roomId, Long requestId, Long targetUserId, ApproveJoinRequestRequest approveRequest) {
        // Fetch room with pessimistic lock to prevent race conditions
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Fetch admin
        User admin = userRepository.findById(approveRequest.getAdminId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        // Validate admin is creator or admin
        boolean isCreator = room.getCreatedBy().getId().equals(approveRequest.getAdminId());
        boolean isAdmin = room.getAdmins().contains(admin);
        if (!isCreator && !isAdmin) {
            throw new ForbiddenException("Only room creator or administrators can approve join requests");
        }

        // Fetch target user
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate user is not already a collaborator
        if (room.getCollaborators().contains(targetUser)) {
            throw new ConflictException("User is already a member of this room");
        }

        // Find the pending request
        Optional<RoomJoinRequest> joinRequestOpt = roomJoinRequestRepository
                .findByRoomIdAndUserIdAndStatus(roomId, targetUserId, "pending");
        if (joinRequestOpt.isEmpty()) {
            throw new ResourceNotFoundException("No pending request found for this user and room");
        }

        RoomJoinRequest joinRequest = joinRequestOpt.get();

        // Validate request ID matches if provided
        if (requestId != null && !joinRequest.getId().equals(requestId)) {
            throw new BadRequestException("Request ID does not match the user's pending request");
        }

        // Check capacity with database lock (already locked via @Lock annotation)
        if (room.getMaxCollaborators() != null) {
            int currentCollaboratorCount = room.getCollaborators().size();
            if (currentCollaboratorCount >= room.getMaxCollaborators()) {
                throw new ConflictException("Room has reached maximum capacity");
            }
        }

        // Update request status
        String previousStatus = joinRequest.getStatus();
        joinRequest.setStatus("approved");
        joinRequest.setRespondedAt(Instant.now());
        joinRequest.setResponder(admin);
        roomJoinRequestRepository.save(joinRequest);

        // Create history entry
        createHistoryEntry(joinRequest, previousStatus, "approved", admin, "Request approved by admin");

        // Add user to collaborators
        room.getCollaborators().add(targetUser);
        roomRepository.save(room);

        // Check if room reached capacity after this approval
        boolean reachedCapacity = room.getMaxCollaborators() != null
                && room.getCollaborators().size() >= room.getMaxCollaborators();

        // If capacity reached, auto-reject all other pending requests
        if (reachedCapacity) {
            autoRejectPendingRequests(roomId, "Room has reached maximum capacity");
        }

        // Send notification to applicant
        sendApprovalNotification(room, joinRequest, targetUser);

        logger.info("Admin {} approved join request {} for user {} to room {}", 
                approveRequest.getAdminId(), joinRequest.getId(), targetUserId, roomId);
    }

    /**
     * Reject a room join request.
     * 
     * Validations:
     * - Admin must be room creator or admin
     * - Request must be pending
     * 
     * Side effects:
     * - Sets lastRejectedAt for cooldown tracking
     * 
     * @param roomId The room ID
     * @param requestId The request ID (optional)
     * @param targetUserId The user ID whose request is being rejected
     * @param rejectRequest The rejection request with adminId and optional reason
     */
    @Transactional
    public void rejectRequest(Long roomId, Long requestId, Long targetUserId, RejectJoinRequestRequest rejectRequest) {
        // Fetch room
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Fetch admin
        User admin = userRepository.findById(rejectRequest.getAdminId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        // Validate admin is creator or admin
        boolean isCreator = room.getCreatedBy().getId().equals(rejectRequest.getAdminId());
        boolean isAdmin = room.getAdmins().contains(admin);
        if (!isCreator && !isAdmin) {
            throw new ForbiddenException("Only room creator or administrators can reject join requests");
        }

        // Fetch target user
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find the pending request
        Optional<RoomJoinRequest> joinRequestOpt = roomJoinRequestRepository
                .findByRoomIdAndUserIdAndStatus(roomId, targetUserId, "pending");
        if (joinRequestOpt.isEmpty()) {
            throw new ResourceNotFoundException("No pending request found for this user and room");
        }

        RoomJoinRequest joinRequest = joinRequestOpt.get();

        // Validate request ID matches if provided
        if (requestId != null && !joinRequest.getId().equals(requestId)) {
            throw new BadRequestException("Request ID does not match the user's pending request");
        }

        // Update request status
        String previousStatus = joinRequest.getStatus();
        joinRequest.setStatus("rejected");
        joinRequest.setRespondedAt(Instant.now());
        joinRequest.setResponder(admin);
        joinRequest.setLastRejectedAt(Instant.now()); // Set for cooldown tracking
        
        // Set rejection reason if provided
        if (rejectRequest.getReason() != null) {
            joinRequest.setRejectionReason(rejectRequest.getReason());
        }

        roomJoinRequestRepository.save(joinRequest);

        // Create history entry
        createHistoryEntry(joinRequest, previousStatus, "rejected", admin, 
                rejectRequest.getReason() != null ? rejectRequest.getReason() : "Request rejected by admin");

        // Send notification to applicant
        sendRejectionNotification(room, joinRequest, targetUser);

        logger.info("Admin {} rejected join request {} for user {} to room {}", 
                rejectRequest.getAdminId(), joinRequest.getId(), targetUserId, roomId);
    }

    /**
     * Cancel a room join request (only the requester can cancel their own request).
     * 
     * Validations:
     * - User must be the request owner
     * - Request must be pending
     * 
     * @param roomId The room ID
     * @param requestId The request ID
     * @param userId The user ID (must be the request owner)
     */
    @Transactional
    public void cancelRequest(Long roomId, Long requestId, Long userId) {
        // Fetch request
        RoomJoinRequest joinRequest = roomJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership request not found"));

        // Validate request belongs to the room
        if (!joinRequest.getRoom().getId().equals(roomId)) {
            throw new BadRequestException("Request does not belong to this room");
        }

        // Validate user is the request owner
        if (!joinRequest.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only cancel your own requests");
        }

        // Validate request is pending
        if (!"pending".equals(joinRequest.getStatus())) {
            throw new BadRequestException("Only pending requests can be cancelled");
        }

        // Update request status
        String previousStatus = joinRequest.getStatus();
        joinRequest.setStatus("cancelled");
        roomJoinRequestRepository.save(joinRequest);

        // Create history entry
        createHistoryEntry(joinRequest, previousStatus, "cancelled", joinRequest.getUser(), "Request cancelled by user");

        // No notification sent per requirements

        logger.info("User {} cancelled membership request {} for room {}", userId, requestId, roomId);
    }

    /**
     * Get all pending requests for a room (admin/creator view).
     */
    public List<RoomJoinRequest> getPendingRequests(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only room creator or admins can see requests
        boolean isCreator = room.getCreatedBy().getId().equals(userId);
        boolean isAdmin = room.getAdmins().contains(user);
        if (!isCreator && !isAdmin) {
            throw new ForbiddenException("Only room creator or admins can view membership requests");
        }

        return roomJoinRequestRepository.findPendingRequestsByRoom(roomId);
    }

    /**
     * Get all pending requests for a user (applicant view).
     */
    public List<RoomJoinRequest> getMyPendingRequests(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return roomJoinRequestRepository.findByUserIdAndStatus(userId, "pending");
    }

    /**
     * Auto-reject all pending requests for a room (used when room closes or reaches capacity).
     */
    @Transactional
    public void autoRejectPendingRequests(Long roomId, String reason) {
        List<RoomJoinRequest> pendingRequests = roomJoinRequestRepository.findPendingRequestsByRoom(roomId);
        
        for (RoomJoinRequest request : pendingRequests) {
            String previousStatus = request.getStatus();
            request.setStatus("rejected");
            request.setRespondedAt(Instant.now());
            request.setLastRejectedAt(Instant.now());
            request.setRejectionReason(reason);
            roomJoinRequestRepository.save(request);

            // Create history entry (system action, no user)
            createHistoryEntry(request, previousStatus, "rejected", null, reason);

            // Send notification to applicant
            sendRejectionNotification(request.getRoom(), request, request.getUser());
        }

        logger.info("Auto-rejected {} pending requests for room {} with reason: {}", 
                pendingRequests.size(), roomId, reason);
    }

    // ========== Private Helper Methods ==========

    private void createHistoryEntry(RoomJoinRequest request, String previousStatus, String newStatus, 
                                     User changedBy, String changeReason) {
        RoomJoinRequestHistory history = new RoomJoinRequestHistory(
                request, previousStatus, newStatus, changedBy, changeReason);
        roomJoinRequestHistoryRepository.save(history);
    }

    private void sendNewRequestNotifications(Room room, RoomJoinRequest request, User requester) {
        // Send to room creator
        sendNotificationToUser(
                room.getCreatedBy().getId(),
                "MEMBERSHIP_REQUEST",
                "Новая заявка на вступление в комнату",
                String.format("Пользователь %s подал заявку на вступление в комнату \"%s\"",
                        requester.getName() != null ? requester.getName() : requester.getUsername(),
                        room.getName() != null ? room.getName() : room.getDescription()),
                String.format("{\"roomId\":%d,\"requestId\":%d,\"requesterId\":%d}",
                        room.getId(), request.getId(), requester.getId()),
                room.getId()
        );

        // Send to all room admins (excluding creator to avoid duplicate)
        for (User admin : room.getAdmins()) {
            if (!admin.getId().equals(room.getCreatedBy().getId())) {
                sendNotificationToUser(
                        admin.getId(),
                        "MEMBERSHIP_REQUEST",
                        "Новая заявка на вступление в комнату",
                        String.format("Пользователь %s подал заявку на вступление в комнату \"%s\"",
                                requester.getName() != null ? requester.getName() : requester.getUsername(),
                                room.getName() != null ? room.getName() : room.getDescription()),
                        String.format("{\"roomId\":%d,\"requestId\":%d,\"requesterId\":%d}",
                                room.getId(), request.getId(), requester.getId()),
                        room.getId()
                );
            }
        }
    }

    private void sendApprovalNotification(Room room, RoomJoinRequest request, User applicant) {
        sendNotificationToUser(
                applicant.getId(),
                "MEMBERSHIP_APPROVED",
                "Заявка одобрена",
                String.format("Ваша заявка на вступление в комнату \"%s\" была одобрена",
                        room.getName() != null ? room.getName() : room.getDescription()),
                String.format("{\"roomId\":%d,\"requestId\":%d}", room.getId(), request.getId()),
                room.getId()
        );
    }

    private void sendRejectionNotification(Room room, RoomJoinRequest request, User applicant) {
        String reasonText = request.getRejectionReason() != null 
                ? " Причина: " + request.getRejectionReason() 
                : "";
        sendNotificationToUser(
                applicant.getId(),
                "MEMBERSHIP_REJECTED",
                "Заявка отклонена",
                String.format("Ваша заявка на вступление в комнату \"%s\" была отклонена.%s",
                        room.getName() != null ? room.getName() : room.getDescription(), reasonText),
                String.format("{\"roomId\":%d,\"requestId\":%d,\"reason\":%s}",
                        room.getId(), request.getId(),
                        request.getRejectionReason() != null ? "\"" + request.getRejectionReason() + "\"" : "null"),
                room.getId()
        );
    }

    private void sendNotificationToUser(Long userId, String type, String title, String content, 
                                        String data, Long roomId) {
        // Note: This will be enhanced in Phase 3 with preference checking and deduplication
        // For now, we just create the notification
        notificationService.createNotification(userId, type, title, content, data);
    }
}

