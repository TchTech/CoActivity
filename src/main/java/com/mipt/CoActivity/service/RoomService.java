package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Message;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.MessageRepository;
import com.mipt.CoActivity.repository.RoomRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private final RoomRepository roomRepository;
  private final UserRepository userRepository;
  private final MessageRepository messageRepository;
  @Autowired
  public RoomService(RoomRepository roomRepository,
                     UserRepository userRepository,
                     MessageRepository messageRepository) {
    this.roomRepository = roomRepository;
    this.userRepository = userRepository;
    this.messageRepository = messageRepository;
  }

  // нужно добавить в метод создания комнаты категории интереса
  public Room createRoom(Long creatorId, String name) {
    User creator =
        userRepository
            .findById(creatorId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    Room room = new Room(creator, name);
    room.getCollaborators().add(creator);
    return roomRepository.save(room);
  }

  public Message addMessage(Long roomId, Long creatorId, String text) {
    User creator =
        userRepository
            .findById(creatorId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    Room room =
        roomRepository
                .findById(roomId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    Message message = new Message(room, creator, text);
    return messageRepository.save(message);
  }

  public void addUserToRoom(Long userId, Long roomId) {

    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    Room room =
        roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("User not found"));

    room.getCollaborators().add(user);
    roomRepository.save(room);
  }

  public void removeUserFromRoom(Long userId, Long roomId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    Room room =
        roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));

    room.getCollaborators().remove(user);
    roomRepository.save(room);
  }
}
