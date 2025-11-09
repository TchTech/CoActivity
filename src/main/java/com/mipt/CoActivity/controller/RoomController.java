package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.Message;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
public class RoomController {

  private final RoomService roomService;

  @Autowired
  public RoomController(RoomService roomService) {
    this.roomService = roomService;
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
