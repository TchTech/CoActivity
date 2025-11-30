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
  private final com.mipt.CoActivity.repository.RoomJoinRequestRepository roomJoinRequestRepository;
  private final com.mipt.CoActivity.repository.NotificationRepository notificationRepository;

  @Autowired
  public RoomService(
          RoomRepository roomRepository,
          UserRepository userRepository,
          MessageRepository messageRepository,
          RoomNotificationSettingsRepository roomNotificationSettingsRepository,
          com.mipt.CoActivity.repository.RoomJoinRequestRepository roomJoinRequestRepository,
          com.mipt.CoActivity.repository.NotificationRepository notificationRepository) {
    this.roomRepository = roomRepository;
    this.userRepository = userRepository;
    this.messageRepository = messageRepository;
    this.roomNotificationSettingsRepository = roomNotificationSettingsRepository;
    this.roomJoinRequestRepository = roomJoinRequestRepository;
    this.notificationRepository = notificationRepository;
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
    room.setJoinType(request.getJoinType() != null ? request.getJoinType() : "open");
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

  @Transactional
  public Message sendRoomMessage(Long roomId, SendMessageRequest request) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User sender = userRepository.findById(request.getSenderId())
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    if (!room.getCollaborators().contains(sender)) {
      throw new ForbiddenException("User is not a member of this room");
    }
    
    Message message = new Message(room, sender, request.getContent());
    message.setDate(Instant.now());
    return messageRepository.save(message);
  }

  @Transactional
  public void reportMessage(Long roomId, Long messageId, ReportMessageRequest request) {
    roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    
    if (!message.getRoom().getId().equals(roomId)) {
      throw new BadRequestException("Message does not belong to this room");
    }
    
    userRepository.findById(request.getReporterId())
        .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));
    
    if (!List.of("platformRules", "roomRules").contains(request.getViolationType())) {
      throw new BadRequestException("Invalid violation type");
    }
    
    logger.info("User {} reported message {} in room {} for violation: {}", 
        request.getReporterId(), messageId, roomId, request.getViolationType());
  }

  @Transactional
  public void confirmMessageViolation(Long roomId, Long messageId, ConfirmViolationRequest request) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    
    if (!message.getRoom().getId().equals(roomId)) {
      throw new BadRequestException("Message does not belong to this room");
    }
    
    User admin = userRepository.findById(request.getAdminId())
        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    
    if (!room.getAdmins().contains(admin)) {
      throw new ForbiddenException("Only administrators can confirm violations");
    }
    
    if (!List.of("warning", "kick", "ban").contains(request.getSanction())) {
      throw new BadRequestException("Invalid sanction type");
    }
    
    message.setIsDeleted(true);
    messageRepository.save(message);
    
    logger.info("Admin {} confirmed violation for message {} in room {} with sanction: {}", 
        request.getAdminId(), messageId, roomId, request.getSanction());
  }

  @Transactional
  public void rejectMessageViolation(Long roomId, Long messageId, RejectViolationRequest request) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    
    if (!message.getRoom().getId().equals(roomId)) {
      throw new BadRequestException("Message does not belong to this room");
    }
    
    User admin = userRepository.findById(request.getAdminId())
        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    
    if (!room.getAdmins().contains(admin)) {
      throw new ForbiddenException("Only administrators can reject violation reports");
    }
    
    logger.info("Admin {} rejected violation report for message {} in room {}", 
        request.getAdminId(), messageId, roomId);
  }

  public List<Room> searchRooms(String query) {
    if (query == null || query.trim().isEmpty()) {
      return roomRepository.findAll();
    }
    
    String searchQuery = query.trim();
    List<Room> byName = roomRepository.findByNameContainingIgnoreCase(searchQuery);
    List<Room> byDescription = roomRepository.findByDescriptionContainingIgnoreCase(searchQuery);
    
    // Combine results and remove duplicates
    java.util.Set<Room> combined = new java.util.HashSet<>(byName);
    combined.addAll(byDescription);
    return new java.util.ArrayList<>(combined);
  }

  @Transactional
  public com.mipt.CoActivity.model.RoomJoinRequest applyToRoom(Long roomId, Long userId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    if (room.getCollaborators().contains(user)) {
      throw new ConflictException("User is already a member of this room");
    }
    
    // Check if there's already a pending request
    java.util.Optional<com.mipt.CoActivity.model.RoomJoinRequest> existingRequest = 
        roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(roomId, userId, "pending");
    if (existingRequest.isPresent()) {
      throw new ConflictException("You have already applied to this room");
    }
    
    com.mipt.CoActivity.model.RoomJoinRequest request = new com.mipt.CoActivity.model.RoomJoinRequest(room, user);
    com.mipt.CoActivity.model.RoomJoinRequest savedRequest = roomJoinRequestRepository.save(request);
    
    // Create notification for room creator
    com.mipt.CoActivity.model.Notification notification = new com.mipt.CoActivity.model.Notification();
    notification.setUser(room.getCreatedBy());
    notification.setTitle("Новая заявка на вступление в комнату");
    notification.setContent(String.format("Пользователь %s подал заявку на вступление в комнату \"%s\"", 
        user.getName() != null ? user.getName() : user.getUsername(), 
        room.getDescription() != null ? room.getDescription() : room.getName()));
    notification.setIsRead(false);
    notification.setCreatedAt(java.time.Instant.now());
    notificationRepository.save(notification);
    
    logger.info("User {} applied to room {}", userId, roomId);
    return savedRequest;
  }

  public List<com.mipt.CoActivity.model.RoomJoinRequest> getPendingJoinRequests(Long roomId) {
    return roomJoinRequestRepository.findByRoomIdAndStatus(roomId, "pending");
  }

  public List<com.mipt.CoActivity.model.RoomJoinRequest> getMyPendingRequests(Long userId) {
    return roomJoinRequestRepository.findByUserIdAndStatus(userId, "pending");
  }

  @Transactional
  public void approveJoinRequest(Long roomId, Long targetUserId, ApproveJoinRequestRequest request) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User admin = userRepository.findById(request.getAdminId())
        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    
    User targetUser = userRepository.findById(targetUserId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    if (!room.getAdmins().contains(admin)) {
      throw new ForbiddenException("Only administrators can approve join requests");
    }
    
    if (room.getCollaborators().contains(targetUser)) {
      throw new ConflictException("User is already a member of this room");
    }
    
    if (room.getMaxCollaborators() != null
        && room.getCollaborators().size() >= room.getMaxCollaborators()) {
      throw new ConflictException("Room has reached maximum capacity");
    }
    
    // Update join request status
    java.util.Optional<com.mipt.CoActivity.model.RoomJoinRequest> joinRequest = 
        roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(roomId, targetUserId, "pending");
    if (joinRequest.isPresent()) {
      joinRequest.get().setStatus("approved");
      roomJoinRequestRepository.save(joinRequest.get());
    }
    
    room.getCollaborators().add(targetUser);
    roomRepository.save(room);
    
    // Create notification for applicant
    com.mipt.CoActivity.model.Notification notification = new com.mipt.CoActivity.model.Notification();
    notification.setUser(targetUser);
    notification.setTitle("Заявка одобрена");
    notification.setContent(String.format("Ваша заявка на вступление в комнату \"%s\" была одобрена", 
        room.getDescription() != null ? room.getDescription() : room.getName()));
    notification.setIsRead(false);
    notification.setCreatedAt(java.time.Instant.now());
    notificationRepository.save(notification);
    
    logger.info("Admin {} approved join request for user {} to room {}", request.getAdminId(), targetUserId, roomId);
  }

  @Transactional
  public void rejectJoinRequest(Long roomId, Long targetUserId, RejectJoinRequestRequest request) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User admin = userRepository.findById(request.getAdminId())
        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    
    User targetUser = userRepository.findById(targetUserId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    if (!room.getAdmins().contains(admin)) {
      throw new ForbiddenException("Only administrators can reject join requests");
    }
    
    // Update join request status
    java.util.Optional<com.mipt.CoActivity.model.RoomJoinRequest> joinRequest = 
        roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(roomId, targetUserId, "pending");
    if (joinRequest.isPresent()) {
      joinRequest.get().setStatus("rejected");
      roomJoinRequestRepository.save(joinRequest.get());
    }
    
    logger.info("Admin {} rejected join request for user {} to room {}", request.getAdminId(), targetUserId, roomId);
  }
}
