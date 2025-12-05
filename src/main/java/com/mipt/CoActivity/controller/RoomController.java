package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.model.Message;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.RoomJoinRequest;
import com.mipt.CoActivity.model.RoomNotificationSettings;
import com.mipt.CoActivity.service.RoomJoinRequestService;
import com.mipt.CoActivity.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for room management and room join request operations.
 * 
 * Provides endpoints for:
 * - Room CRUD operations
 * - Room join request lifecycle (create, approve, reject, cancel)
 * - Room closure
 * - Room chat and messaging
 */
@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class RoomController {

  private final RoomService roomService;
  private final RoomJoinRequestService roomJoinRequestService;

  @Autowired
  public RoomController(RoomService roomService, RoomJoinRequestService roomJoinRequestService) {
    this.roomService = roomService;
    this.roomJoinRequestService = roomJoinRequestService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Room> createRoom(
      @RequestParam Long userId, @RequestBody CreateRoomRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(roomService.createRoom(userId, request));
  }

  @PostMapping("/{roomId}/join")
  public ResponseEntity<Void> joinRoom(
      @PathVariable Long roomId, @RequestBody JoinRoomRequest request) {
    roomService.joinRoom(roomId, request);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{roomId}/chat")
  public ResponseEntity<RoomChatResponse> openRoomChat(
      @PathVariable Long roomId, @RequestParam Long userId) {
    return ResponseEntity.ok(roomService.openRoomChat(roomId, userId));
  }

  @GetMapping("/{roomId}")
  public ResponseEntity<RoomDetailsResponse> getRoomDetails(@PathVariable Long roomId) {
    RoomDetailsResponse response = roomService.getRoomDetails(roomId);
    System.out.println("[RoomController] Returning room details, members count: " + (response.getMembers() != null ? response.getMembers().size() : "null"));
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{roomId}/brief")
  public ResponseEntity<RoomBriefResponse> getRoomBrief(@PathVariable Long roomId) {
    return ResponseEntity.ok(roomService.getRoomBrief(roomId));
  }

  @GetMapping("/{roomId}/settings/notifications")
  public ResponseEntity<RoomNotificationSettings> getRoomNotificationSettings(
      @PathVariable Long roomId) {
    return ResponseEntity.ok(roomService.getRoomNotificationSettings(roomId));
  }

  @PutMapping("/{roomId}/settings/notifications")
  public ResponseEntity<RoomNotificationSettings> updateRoomNotificationSettings(
      @PathVariable Long roomId, @RequestBody RoomNotificationSettingsRequest request) {
    return ResponseEntity.ok(roomService.updateRoomNotificationSettings(roomId, request));
  }

  @PostMapping("/create")
  @ResponseStatus(HttpStatus.CREATED)
  public Room createRoom(@RequestParam Long userId, @RequestParam String name) {
    return roomService.createRoom(userId, name);
  }

  @PostMapping("/{roomId}/participants/add")
  public void addUserToRoom(@PathVariable Long roomId, @RequestParam Long userId) {
    roomService.addUserToRoom(userId, roomId);
  }

  @DeleteMapping("/{roomId}/participants/remove")
  public void removeUserFromRoom(@PathVariable Long roomId, @RequestParam Long userId) {
    roomService.removeUserFromRoom(userId, roomId);
  }

  @PostMapping("/{roomId}/messages/add")
  public Message createMessage(
      @PathVariable Long roomId, @RequestParam Long creatorId, @RequestParam String text) {
    return roomService.addMessage(roomId, creatorId, text);
  }

  // Legacy endpoints removed - use new endpoints below

  @PostMapping("/{roomId}/chat/messages")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Message> sendRoomMessage(
      @PathVariable Long roomId, @RequestBody SendMessageRequest request) {
    Message message = roomService.sendRoomMessage(roomId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(message);
  }

  @PostMapping("/{roomId}/chat/messages/{messageId}/report")
  public ResponseEntity<Void> reportMessage(
      @PathVariable Long roomId,
      @PathVariable Long messageId,
      @RequestBody ReportMessageRequest request) {
    roomService.reportMessage(roomId, messageId, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{roomId}/chat/messages/{messageId}/moderation/confirm")
  public ResponseEntity<Void> confirmMessageViolation(
      @PathVariable Long roomId,
      @PathVariable Long messageId,
      @RequestBody ConfirmViolationRequest request) {
    roomService.confirmMessageViolation(roomId, messageId, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{roomId}/chat/messages/{messageId}/moderation/reject")
  public ResponseEntity<Void> rejectMessageViolation(
      @PathVariable Long roomId,
      @PathVariable Long messageId,
      @RequestBody RejectViolationRequest request) {
    roomService.rejectMessageViolation(roomId, messageId, request);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/search")
  public ResponseEntity<List<Room>> searchRooms(@RequestParam String query) {
    return ResponseEntity.ok(roomService.searchRooms(query));
  }

  // ========== Room Join Request Endpoints ==========

  /**
   * Create a new room join request.
   * 
   * Validations:
   * - Room must exist and have joinType = "by_application"
   * - User must not already be a collaborator
   * - User must not have a pending request
   * - 5-minute cooldown after rejection must have passed
   * 
   * @param roomId The room ID
   * @param userId The user ID requesting to join
   * @param request Optional request body with message (max 500 characters)
   * @return Created RoomJoinRequest
   * 
   * @apiNote POST /api/rooms/{roomId}/requests?userId={userId}
   * @response 201 Created - Request created successfully
   * @response 400 Bad Request - Invalid room type, cooldown active, or validation error
   * @response 404 Not Found - Room or user not found
   * @response 409 Conflict - User already member or pending request exists
   */
  @PostMapping("/{roomId}/requests")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<RoomJoinRequestResponse> createRequest(
      @PathVariable Long roomId,
      @RequestParam(required = true) Long userId,
      @RequestBody(required = false) @Valid CreateRoomJoinRequestRequest request) {
    MembershipRequestRequest membershipRequest = null;
    if (request != null) {
      membershipRequest = new MembershipRequestRequest();
      membershipRequest.setMessage(request.getMessage());
    }
    
    RoomJoinRequest createdRequest = roomJoinRequestService.createRequest(roomId, userId, membershipRequest);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapToResponse(createdRequest));
  }

  /**
   * Get all pending requests for a room (admin/creator view).
   * 
   * Only room creator or admins can view requests.
   * 
   * @param roomId The room ID
   * @param userId The user ID (must be creator or admin)
   * @return List of pending requests
   * 
   * @apiNote GET /api/rooms/{roomId}/requests?userId={userId}
   * @response 200 OK - List of requests
   * @response 403 Forbidden - User is not creator or admin
   * @response 404 Not Found - Room or user not found
   */
  @GetMapping("/{roomId}/requests")
  public ResponseEntity<List<RoomJoinRequestResponse>> getPendingRequests(
      @PathVariable Long roomId,
      @RequestParam(required = true) Long userId) {
    List<RoomJoinRequest> requests = roomJoinRequestService.getPendingRequests(roomId, userId);
    List<RoomJoinRequestResponse> responses = requests.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  /**
   * Approve a room join request.
   * 
   * Validations:
   * - Admin must be room creator or admin
   * - Request must be pending
   * - User must not already be a collaborator
   * - Room must have capacity (with database locking)
   * 
   * Side effects:
   * - If capacity reached, auto-rejects all other pending requests
   * 
   * @param roomId The room ID
   * @param requestId The request ID
   * @param targetUserId The user ID whose request is being approved
   * @param request Request body with adminId
   * @return 200 OK
   * 
   * @apiNote POST /api/rooms/{roomId}/requests/{requestId}/approve?targetUserId={targetUserId}
   * @response 200 OK - Request approved successfully
   * @response 400 Bad Request - Request not pending, user already member, or room at capacity
   * @response 403 Forbidden - User is not creator or admin
   * @response 404 Not Found - Room, request, or user not found
   */
  @PostMapping("/{roomId}/requests/{requestId}/approve")
  public ResponseEntity<Void> approveRequest(
      @PathVariable Long roomId,
      @PathVariable Long requestId,
      @RequestParam(required = true) Long targetUserId,
      @RequestBody @Valid ApproveJoinRequestRequest request) {
    roomJoinRequestService.approveRequest(roomId, requestId, targetUserId, request);
    return ResponseEntity.ok().build();
  }

  /**
   * Reject a room join request.
   * 
   * Validations:
   * - Admin must be room creator or admin
   * - Request must be pending
   * 
   * Side effects:
   * - Sets lastRejectedAt for 5-minute cooldown tracking
   * 
   * @param roomId The room ID
   * @param requestId The request ID
   * @param targetUserId The user ID whose request is being rejected
   * @param request Request body with adminId and optional reason
   * @return 200 OK
   * 
   * @apiNote POST /api/rooms/{roomId}/requests/{requestId}/reject?targetUserId={targetUserId}
   * @response 200 OK - Request rejected successfully
   * @response 400 Bad Request - Request not pending or validation error
   * @response 403 Forbidden - User is not creator or admin
   * @response 404 Not Found - Room, request, or user not found
   */
  @PostMapping("/{roomId}/requests/{requestId}/reject")
  public ResponseEntity<Void> rejectRequest(
      @PathVariable Long roomId,
      @PathVariable Long requestId,
      @RequestParam(required = true) Long targetUserId,
      @RequestBody @Valid RejectJoinRequestRequest request) {
    roomJoinRequestService.rejectRequest(roomId, requestId, targetUserId, request);
    return ResponseEntity.ok().build();
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
   * @return 200 OK
   * 
   * @apiNote DELETE /api/rooms/{roomId}/requests/{requestId}?userId={userId}
   * @response 200 OK - Request cancelled successfully
   * @response 400 Bad Request - Request not pending
   * @response 403 Forbidden - User is not the request owner
   * @response 404 Not Found - Request not found
   */
  @DeleteMapping("/{roomId}/requests/{requestId}")
  public ResponseEntity<Void> cancelRequest(
      @PathVariable Long roomId,
      @PathVariable Long requestId,
      @RequestParam(required = true) Long userId) {
    roomJoinRequestService.cancelRequest(roomId, requestId, userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Get all pending requests for the current user (applicant view).
   * 
   * @param userId The user ID
   * @return List of user's pending requests
   * 
   * @apiNote GET /api/rooms/my-applications?userId={userId}
   * @response 200 OK - List of user's pending requests
   * @response 404 Not Found - User not found
   */
  @GetMapping("/my-applications")
  public ResponseEntity<List<RoomJoinRequestResponse>> getMyPendingRequests(
      @RequestParam(required = true) Long userId) {
    List<RoomJoinRequest> requests = roomJoinRequestService.getMyPendingRequests(userId);
    List<RoomJoinRequestResponse> responses = requests.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  /**
   * Manually close a room.
   * 
   * Validations:
   * - User must be room creator or admin
   * - Room must not already be closed
   * 
   * Side effects:
   * - Sets room as closed
   * - Auto-rejects all pending requests
   * - Sends notifications to all applicants
   * 
   * @param roomId The room ID
   * @param request Request body with userId
   * @return 200 OK
   * 
   * @apiNote POST /api/rooms/{roomId}/close
   * @response 200 OK - Room closed successfully
   * @response 400 Bad Request - Room already closed
   * @response 403 Forbidden - User is not creator or admin
   * @response 404 Not Found - Room or user not found
   */
  @PostMapping("/{roomId}/close")
  public ResponseEntity<Void> closeRoom(
      @PathVariable Long roomId,
      @RequestBody @Valid CloseRoomRequest request) {
    roomService.closeRoom(roomId, request.getUserId());
    return ResponseEntity.ok().build();
  }

  // ========== Legacy Endpoints (for backward compatibility) ==========

  @PostMapping("/{roomId}/apply")
  @ResponseStatus(HttpStatus.CREATED)
  @Deprecated
  public ResponseEntity<RoomJoinRequest> applyToRoom(
      @PathVariable Long roomId, @RequestBody @Valid JoinRoomRequest request) {
    MembershipRequestRequest membershipRequest = new MembershipRequestRequest();
    RoomJoinRequest createdRequest = roomJoinRequestService.createRequest(roomId, request.getUserId(), membershipRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
  }

  @PostMapping("/{roomId}/join-requests/{targetUserId}/approve")
  @Deprecated
  public ResponseEntity<Void> approveJoinRequest(
      @PathVariable Long roomId,
      @PathVariable Long targetUserId,
      @RequestBody @Valid ApproveJoinRequestRequest request) {
    roomJoinRequestService.approveRequest(roomId, null, targetUserId, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{roomId}/join-requests/{targetUserId}/reject")
  @Deprecated
  public ResponseEntity<Void> rejectJoinRequest(
      @PathVariable Long roomId,
      @PathVariable Long targetUserId,
      @RequestBody @Valid RejectJoinRequestRequest request) {
    roomJoinRequestService.rejectRequest(roomId, null, targetUserId, request);
    return ResponseEntity.ok().build();
  }

  // ========== Helper Methods ==========

  /**
   * Map RoomJoinRequest entity to RoomJoinRequestResponse DTO.
   */
  private RoomJoinRequestResponse mapToResponse(RoomJoinRequest request) {
    return RoomJoinRequestResponse.fromEntity(request);
  }

  @PostMapping("/{roomId}/pinned-posts")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<com.mipt.CoActivity.model.RoomPostPin> pinPostToRoom(
      @PathVariable Long roomId, @RequestParam Long userId, @RequestBody PinPostRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(roomService.pinPostToRoom(roomId, request.getPostId(), userId));
  }

  @DeleteMapping("/{roomId}/pinned-posts/{postId}")
  public ResponseEntity<Void> unpinPostFromRoom(
      @PathVariable Long roomId, @PathVariable Integer postId, @RequestParam Long userId) {
    roomService.unpinPostFromRoom(roomId, postId, userId);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{roomId}/pinned-posts")
  public ResponseEntity<List<com.mipt.CoActivity.model.Post>> getPinnedPosts(@PathVariable Long roomId) {
    return ResponseEntity.ok(roomService.getPinnedPosts(roomId));
  }

  @GetMapping
  public ResponseEntity<List<Room>> getAllRooms(
      @RequestParam(required = false, defaultValue = "0") Integer offset,
      @RequestParam(required = false, defaultValue = "50") Integer limit) {
    return ResponseEntity.ok(roomService.getAllRooms(offset, limit));
  }

  @PostMapping("/{roomId}/admin/kick")
  public ResponseEntity<Void> kickUserFromRoom(
      @PathVariable Long roomId,
      @RequestParam Long userIdToKick,
      @RequestParam Long adminUserId) {
    roomService.kickUserFromRoom(roomId, userIdToKick, adminUserId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{roomId}/chat/messages/{messageId}")
  public ResponseEntity<Void> deleteMessage(
      @PathVariable Long roomId,
      @PathVariable Long messageId,
      @RequestParam Long adminUserId) {
    roomService.deleteMessage(roomId, messageId, adminUserId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{roomId}/admin/promote")
  public ResponseEntity<Void> promoteToAdmin(
      @PathVariable Long roomId,
      @RequestParam Long userIdToPromote,
      @RequestParam Long adminUserId) {
    roomService.promoteToAdmin(roomId, userIdToPromote, adminUserId);
    return ResponseEntity.ok().build();
  }
}
