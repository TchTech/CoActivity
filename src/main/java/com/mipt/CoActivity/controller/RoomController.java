package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.model.Message;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.RoomNotificationSettings;
import com.mipt.CoActivity.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class RoomController {

  private final RoomService roomService;

  @Autowired
  public RoomController(RoomService roomService) {
    this.roomService = roomService;
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
    return ResponseEntity.ok(roomService.getRoomDetails(roomId));
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

  @PostMapping("/{roomId}/join-requests/{targetUserId}/approve")
  public ResponseEntity<Void> approveJoinRequest(
      @PathVariable Long roomId,
      @PathVariable Long targetUserId,
      @RequestBody ApproveJoinRequestRequest request) {
    roomService.approveJoinRequest(roomId, targetUserId, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{roomId}/join-requests/{targetUserId}/reject")
  public ResponseEntity<Void> rejectJoinRequest(
      @PathVariable Long roomId,
      @PathVariable Long targetUserId,
      @RequestBody RejectJoinRequestRequest request) {
    roomService.rejectJoinRequest(roomId, targetUserId, request);
    return ResponseEntity.ok().build();
  }

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

  @PostMapping("/{roomId}/apply")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<com.mipt.CoActivity.model.RoomJoinRequest> applyToRoom(
      @PathVariable Long roomId, @RequestBody com.mipt.CoActivity.dto.JoinRoomRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(roomService.applyToRoom(roomId, request.getUserId()));
  }

  @PostMapping("/{roomId}/requests")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<com.mipt.CoActivity.model.RoomJoinRequest> createMembershipRequest(
      @PathVariable Long roomId, @RequestParam Long userId, @RequestBody(required = false) MembershipRequestRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(roomService.createMembershipRequest(roomId, userId, request));
  }

  @GetMapping("/{roomId}/requests")
  public ResponseEntity<List<com.mipt.CoActivity.model.RoomJoinRequest>> getMembershipRequests(
      @PathVariable Long roomId, @RequestParam Long userId) {
    return ResponseEntity.ok(roomService.getMembershipRequests(roomId, userId));
  }

  @DeleteMapping("/{roomId}/requests/{requestId}")
  public ResponseEntity<Void> cancelMembershipRequest(
      @PathVariable Long roomId, @PathVariable Long requestId, @RequestParam Long userId) {
    roomService.cancelMembershipRequest(roomId, requestId, userId);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{roomId}/join-requests")
  public ResponseEntity<List<com.mipt.CoActivity.model.RoomJoinRequest>> getPendingJoinRequests(
      @PathVariable Long roomId) {
    return ResponseEntity.ok(roomService.getPendingJoinRequests(roomId));
  }

  @GetMapping("/my-applications")
  public ResponseEntity<List<com.mipt.CoActivity.model.RoomJoinRequest>> getMyPendingRequests(
      @RequestParam Long userId) {
    return ResponseEntity.ok(roomService.getMyPendingRequests(userId));
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
}
