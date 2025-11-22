package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.exception.*;
import com.mipt.CoActivity.model.*;
import com.mipt.CoActivity.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {
  private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

  private final RoomRepository roomRepository;
  private final UserRepository userRepository;
  private final MessageRepository messageRepository;
  private final RoomNotificationSettingsRepository roomNotificationSettingsRepository;

  @Autowired
  public RoomService(
          RoomRepository roomRepository,
          UserRepository userRepository,
          MessageRepository messageRepository,
          RoomNotificationSettingsRepository roomNotificationSettingsRepository) {
    this.roomRepository = roomRepository;
    this.userRepository = userRepository;
    this.messageRepository = messageRepository;
    this.roomNotificationSettingsRepository = roomNotificationSettingsRepository;
  }

  @Transactional
  public Room createRoom(Long creatorId, CreateRoomRequest request) {
    User creator =
            userRepository
                    .findById(creatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (request.getMaxCollaborators() != null && request.getMaxCollaborators() <= 0) {
      throw new BadRequestException("Max collaborators must be greater than 0");
    }

    String roomName = request.getDescription() != null && !request.getDescription().isEmpty()
            ? request.getDescription().substring(0, Math.min(50, request.getDescription().length()))
            : "New Room";
    Room room = new Room(creator, roomName);
    room.setDescription(request.getDescription());
    room.setCategory(request.getCategory());
    room.setMaxCollaborators(request.getMaxCollaborators());
    room.setMeetingTime(request.getMeetingTime());
    room.setMeetingType(request.getMeetingType());
    room.setLocation(request.getLocation());
    room.getCollaborators().add(creator);
    room.getAdmins().add(creator);

    Room savedRoom = roomRepository.save(room);

    RoomNotificationSettings settings = new RoomNotificationSettings(savedRoom);
    roomNotificationSettingsRepository.save(settings);

    return savedRoom;
  }

  @Transactional
  public void joinRoom(Long roomId, JoinRoomRequest request) {
    Room room =
            roomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    User user =
            userRepository
                    .findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (room.getCollaborators().contains(user)) {
      throw new ConflictException("User is already a member of this room");
    }

    if (room.getMaxCollaborators() != null
            && room.getCollaborators().size() >= room.getMaxCollaborators()) {
      throw new ConflictException("Room has reached maximum capacity");
    }

    room.getCollaborators().add(user);
    roomRepository.save(room);
    logger.info("User {} joined room {}", request.getUserId(), roomId);
  }

  public RoomChatResponse openRoomChat(Long roomId, Long userId) {
    Room room =
            roomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (!room.getCollaborators().contains(user)) {
      throw new ForbiddenException("User is not a member of this room");
    }

    List<Message> messages = messageRepository.findByRoomId(roomId);

    RoomChatResponse response = new RoomChatResponse();
    response.setRoomId(roomId);
    response.setMessages(
            messages.stream()
                    .filter(msg -> !Boolean.TRUE.equals(msg.getIsDeleted()))
                    .map(
                            msg -> {
                              ChatMessageResponse chatMsg = new ChatMessageResponse();
                              chatMsg.setSenderId(msg.getAuthor().getId());
                              chatMsg.setContent(msg.getText());
                              chatMsg.setTimestamp(msg.getDateCreated());
                              return chatMsg;
                            })
                    .collect(Collectors.toList()));

    return response;
  }

  public RoomBriefResponse getRoomBrief(Long roomId) {
    Room room =
            roomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    RoomBriefResponse response = new RoomBriefResponse();
    response.setRoomId(roomId);
    response.setBriefDescription(room.getDescription());

    return response;
  }

  public RoomNotificationSettings getRoomNotificationSettings(Long roomId) {
    Room room =
            roomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    return roomNotificationSettingsRepository
            .findByRoom(room)
            .orElseGet(
                    () -> {
                      RoomNotificationSettings settings = new RoomNotificationSettings(room);
                      return roomNotificationSettingsRepository.save(settings);
                    });
  }

  @Transactional
  public RoomNotificationSettings updateRoomNotificationSettings(
          Long roomId, RoomNotificationSettingsRequest request) {
    Room room =
            roomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    RoomNotificationSettings settings =
            roomNotificationSettingsRepository
                    .findByRoom(room)
                    .orElseGet(
                            () -> {
                              RoomNotificationSettings newSettings = new RoomNotificationSettings(room);
                              return roomNotificationSettingsRepository.save(newSettings);
                            });

    if (request.getInvitationNotifications() != null) {
      settings.setInvitationNotifications(request.getInvitationNotifications());
    }
    if (request.getMessageNotifications() != null) {
      settings.setMessageNotifications(request.getMessageNotifications());
    }
    if (request.getRemovalNotifications() != null) {
      settings.setRemovalNotifications(request.getRemovalNotifications());
    }

    return roomNotificationSettingsRepository.save(settings);
  }

  public Message addMessage(Long roomId, Long creatorId, String text) {
    User creator =
            userRepository
                    .findById(creatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Room room =
            roomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    if (!room.getCollaborators().contains(creator)) {
      throw new ForbiddenException("User is not a member of this room");
    }

    Message message = new Message(room, creator, text);
    message.setDate(Instant.now());
    return messageRepository.save(message);
  }

  @Transactional
  public void addUserToRoom(Long userId, Long roomId) {
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Room room =
            roomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    if (room.getMaxCollaborators() != null
            && room.getCollaborators().size() >= room.getMaxCollaborators()) {
      throw new ConflictException("Room has reached maximum capacity");
    }

    if (room.getCollaborators().contains(user)) {
      throw new ConflictException("User is already a member of this room");
    }

    room.getCollaborators().add(user);
    roomRepository.save(room);
  }

  @Transactional
  public void removeUserFromRoom(Long userId, Long roomId) {
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Room room =
            roomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    if (!room.getCollaborators().contains(user)) {
      throw new BadRequestException("User is not a member of this room");
    }

    room.getCollaborators().remove(user);
    roomRepository.save(room);
  }

  public Room createRoom(Long creatorId, String name) {
    User creator =
            userRepository
                    .findById(creatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Room room = new Room(creator, name);
    room.getCollaborators().add(creator);
    return roomRepository.save(room);
  }
}
