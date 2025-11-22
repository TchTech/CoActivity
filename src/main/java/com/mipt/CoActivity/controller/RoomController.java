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

@RestController
@RequestMapping("/rooms")
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
}
