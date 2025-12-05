package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.exception.*;
import com.mipt.CoActivity.model.*;
import com.mipt.CoActivity.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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
  private final com.mipt.CoActivity.repository.RoomPostPinRepository roomPostPinRepository;
  private final com.mipt.CoActivity.repository.PostRepository postRepository;
  private final NotificationService notificationService;

  @Autowired
  public RoomService(
          RoomRepository roomRepository,
          UserRepository userRepository,
          MessageRepository messageRepository,
          RoomNotificationSettingsRepository roomNotificationSettingsRepository,
          com.mipt.CoActivity.repository.RoomJoinRequestRepository roomJoinRequestRepository,
          com.mipt.CoActivity.repository.RoomPostPinRepository roomPostPinRepository,
          com.mipt.CoActivity.repository.PostRepository postRepository,
          NotificationService notificationService) {
    this.roomRepository = roomRepository;
    this.userRepository = userRepository;
    this.messageRepository = messageRepository;
    this.roomNotificationSettingsRepository = roomNotificationSettingsRepository;
    this.roomJoinRequestRepository = roomJoinRequestRepository;
    this.roomPostPinRepository = roomPostPinRepository;
    this.postRepository = postRepository;
    this.notificationService = notificationService;
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
                              chatMsg.setId(msg.getId());
                              chatMsg.setSenderId(msg.getAuthor().getId());
                              chatMsg.setContent(msg.getText());
                              chatMsg.setTimestamp(msg.getDateCreated());
                              chatMsg.setSenderName(msg.getAuthor().getName() != null ? msg.getAuthor().getName() : msg.getAuthor().getUsername());
                              if (msg.getAuthor().getAvatar() != null) {
                                  ChatMessageResponse.SenderAvatar avatar = new ChatMessageResponse.SenderAvatar();
                                  avatar.setId(msg.getAuthor().getAvatar().getId());
                                  chatMsg.setSenderAvatar(avatar);
                              }
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
    
    // For default room, ensure user is a member (auto-add if needed)
    if (Boolean.TRUE.equals(room.getIsDefault())) {
      if (!room.getCollaborators().contains(sender)) {
        logger.info("Auto-adding user {} to default room {}", sender.getId(), roomId);
        room.getCollaborators().add(sender);
        roomRepository.save(room);
      }
    } else {
      // For non-default rooms, check membership strictly
      if (!room.getCollaborators().contains(sender)) {
        throw new ForbiddenException("User is not a member of this room");
      }
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
    String notificationData = String.format("{\"roomId\":%d,\"requestId\":%d,\"requesterId\":%d}", 
        roomId, savedRequest.getId(), userId);
    notificationService.createNotification(
        room.getCreatedBy().getId(),
        "MEMBERSHIP_REQUEST",
        "Новая заявка на вступление в комнату",
        String.format("Пользователь %s подал заявку на вступление в комнату \"%s\"", 
            user.getName() != null ? user.getName() : user.getUsername(), 
            room.getName() != null ? room.getName() : room.getDescription()),
        notificationData
    );
    
    logger.info("User {} applied to room {}", userId, roomId);
    return savedRequest;
  }

  @Transactional
  public com.mipt.CoActivity.model.RoomJoinRequest createMembershipRequest(Long roomId, Long userId, MembershipRequestRequest request) {
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
    
    com.mipt.CoActivity.model.RoomJoinRequest joinRequest = new com.mipt.CoActivity.model.RoomJoinRequest(room, user);
    if (request != null && request.getMessage() != null) {
      joinRequest.setMessage(request.getMessage());
    }
    com.mipt.CoActivity.model.RoomJoinRequest savedRequest = roomJoinRequestRepository.save(joinRequest);
    
    // Create notification for room creator
    String notificationData = String.format("{\"roomId\":%d,\"requestId\":%d,\"requesterId\":%d}", 
        roomId, savedRequest.getId(), userId);
    notificationService.createNotification(
        room.getCreatedBy().getId(),
        "MEMBERSHIP_REQUEST",
        "Новая заявка на вступление в комнату",
        String.format("Пользователь %s подал заявку на вступление в комнату \"%s\"", 
            user.getName() != null ? user.getName() : user.getUsername(), 
            room.getName() != null ? room.getName() : room.getDescription()),
        notificationData
    );
    
    logger.info("User {} created membership request for room {}", userId, roomId);
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
      joinRequest.get().setRespondedAt(Instant.now());
      joinRequest.get().setResponder(admin);
      roomJoinRequestRepository.save(joinRequest.get());
      
      room.getCollaborators().add(targetUser);
      roomRepository.save(room);
      
      // Create notification for applicant
      String notificationData = String.format("{\"roomId\":%d,\"requestId\":%d}", roomId, joinRequest.get().getId());
      notificationService.createNotification(
          targetUserId,
          "MEMBERSHIP_APPROVED",
          "Заявка одобрена",
          String.format("Ваша заявка на вступление в комнату \"%s\" была одобрена", 
              room.getName() != null ? room.getName() : room.getDescription()),
          notificationData
      );
      
      logger.info("Admin {} approved join request for user {} to room {}", request.getAdminId(), targetUserId, roomId);
    }
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
      joinRequest.get().setRespondedAt(Instant.now());
      joinRequest.get().setResponder(admin);
      roomJoinRequestRepository.save(joinRequest.get());
      
      // Create notification for applicant
      String notificationData = String.format("{\"roomId\":%d,\"requestId\":%d}", roomId, joinRequest.get().getId());
      notificationService.createNotification(
          targetUserId,
          "MEMBERSHIP_REJECTED",
          "Заявка отклонена",
          String.format("Ваша заявка на вступление в комнату \"%s\" была отклонена", 
              room.getName() != null ? room.getName() : room.getDescription()),
          notificationData
      );
      
      logger.info("Admin {} rejected join request for user {} to room {}", request.getAdminId(), targetUserId, roomId);
    }
  }

  public List<com.mipt.CoActivity.model.RoomJoinRequest> getMembershipRequests(Long roomId, Long userId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // Only room creator or admins can see requests
    if (!room.getCreatedBy().getId().equals(userId) && !room.getAdmins().contains(user)) {
      throw new ForbiddenException("Only room creator or admins can view membership requests");
    }
    
    return roomJoinRequestRepository.findByRoomId(roomId);
  }

  @Transactional
  public void cancelMembershipRequest(Long roomId, Long requestId, Long userId) {
    com.mipt.CoActivity.model.RoomJoinRequest joinRequest = roomJoinRequestRepository.findById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Membership request not found"));
    
    if (!joinRequest.getRoom().getId().equals(roomId)) {
      throw new BadRequestException("Request does not belong to this room");
    }
    
    if (!joinRequest.getUser().getId().equals(userId)) {
      throw new ForbiddenException("You can only cancel your own requests");
    }
    
    if (!"pending".equals(joinRequest.getStatus())) {
      throw new BadRequestException("Only pending requests can be cancelled");
    }
    
    joinRequest.setStatus("cancelled");
    roomJoinRequestRepository.save(joinRequest);
    
    logger.info("User {} cancelled membership request {} for room {}", userId, requestId, roomId);
  }

  @Transactional
  public RoomPostPin pinPostToRoom(Long roomId, Integer postId, Long userId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // Only room creator or admins can pin posts
    if (!room.getCreatedBy().getId().equals(userId) && !room.getAdmins().contains(user)) {
      throw new ForbiddenException("Only room creator or admins can pin posts");
    }
    
    // Check if already pinned
    java.util.Optional<RoomPostPin> existingPin = roomPostPinRepository.findByRoomIdAndPostId(roomId, postId);
    if (existingPin.isPresent()) {
      throw new ConflictException("Post is already pinned to this room");
    }
    
    RoomPostPin pin = new RoomPostPin(room, post, user);
    RoomPostPin savedPin = roomPostPinRepository.save(pin);
    
    // Create notification for room members (optional - can be limited to post author)
    String notificationData = String.format("{\"roomId\":%d,\"postId\":%d}", roomId, postId);
    if (post.getAuthor() != null && !post.getAuthor().getId().equals(userId)) {
      notificationService.createNotification(
          post.getAuthor().getId(),
          "POST_PINNED",
          "Пост закреплен в комнате",
          String.format("Ваш пост \"%s\" был закреплен в комнате \"%s\"", 
              post.getName() != null ? post.getName() : "пост",
              room.getName() != null ? room.getName() : room.getDescription()),
          notificationData
      );
    }
    
    logger.info("User {} pinned post {} to room {}", userId, postId, roomId);
    return savedPin;
  }

  @Transactional
  public void unpinPostFromRoom(Long roomId, Integer postId, Long userId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // Only room creator or admins can unpin posts
    if (!room.getCreatedBy().getId().equals(userId) && !room.getAdmins().contains(user)) {
      throw new ForbiddenException("Only room creator or admins can unpin posts");
    }
    
    java.util.Optional<RoomPostPin> pin = roomPostPinRepository.findByRoomIdAndPostId(roomId, postId);
    if (!pin.isPresent()) {
      throw new ResourceNotFoundException("Post is not pinned to this room");
    }
    
    roomPostPinRepository.delete(pin.get());
    
    logger.info("User {} unpinned post {} from room {}", userId, postId, roomId);
  }

  public List<Post> getPinnedPosts(Long roomId) {
    // Verify room exists
    roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    List<RoomPostPin> pins = roomPostPinRepository.findByRoomId(roomId);
    return pins.stream()
        .map(RoomPostPin::getPost)
        .collect(Collectors.toList());
  }

  public RoomDetailsResponse getRoomDetails(Long roomId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    RoomDetailsResponse response = new RoomDetailsResponse();
    response.setId(room.getId());
    response.setName(room.getName());
    response.setDescription(room.getDescription());
    response.setCategory(room.getCategory());
    response.setCreatorId(room.getCreatedBy() != null ? room.getCreatedBy().getId() : null);
    response.setCreatorName(room.getCreatedBy() != null ? 
        (room.getCreatedBy().getName() != null ? room.getCreatedBy().getName() : room.getCreatedBy().getUsername()) : null);
    // Set creator avatar if exists
    if (room.getCreatedBy() != null && room.getCreatedBy().getAvatar() != null) {
      RoomDetailsResponse.CreatorAvatar creatorAvatar = new RoomDetailsResponse.CreatorAvatar();
      creatorAvatar.setId(room.getCreatedBy().getAvatar().getId());
      response.setCreatorAvatar(creatorAvatar);
    }
    response.setLocation(room.getLocation());
    response.setMemberCount(room.getCollaborators() != null ? room.getCollaborators().size() : 0);
    response.setPinnedPostCount(room.getPinnedPosts() != null ? room.getPinnedPosts().size() : 0);
    response.setUpdatedAt(room.getCreatedAt()); // Use createdAt as updatedAt for now
    response.setCreatedAt(room.getCreatedAt());
    response.setMeetingType(room.getMeetingType());
    response.setMeetingTime(room.getMeetingTime());
    response.setMaxCollaborators(room.getMaxCollaborators());
    response.setJoinType(room.getJoinType() != null ? room.getJoinType() : "open");
    response.setIsDefault(room.getIsDefault() != null ? room.getIsDefault() : false);
    
    // Fill members list
    List<RoomDetailsResponse.RoomMemberInfo> membersList = new ArrayList<>();
    if (room.getCollaborators() != null && !room.getCollaborators().isEmpty()) {
      logger.info("Room {} has {} collaborators", roomId, room.getCollaborators().size());
      membersList = room.getCollaborators().stream()
          .map(user -> {
            RoomDetailsResponse.RoomMemberInfo memberInfo = new RoomDetailsResponse.RoomMemberInfo();
            memberInfo.setId(user.getId());
            memberInfo.setUsername(user.getUsername());
            memberInfo.setName(user.getName() != null ? user.getName() : user.getUsername());
            // Check if user is creator or admin
            boolean isCreator = room.getCreatedBy() != null && room.getCreatedBy().getId().equals(user.getId());
            boolean isAdmin = room.getAdmins() != null && room.getAdmins().stream()
                .anyMatch(admin -> admin.getId().equals(user.getId()));
            memberInfo.setIsAdmin(isCreator || isAdmin);
            // Calculate average rating from feedbacks
            // Note: feedbacks might be lazy-loaded, so we need to ensure it's loaded
            try {
              if (user.getFeedbacks() != null && !user.getFeedbacks().isEmpty()) {
                double avgRating = user.getFeedbacks().stream()
                    .filter(f -> f != null && f.getRating() != null)
                    .mapToDouble(f -> f.getRating())
                    .average()
                    .orElse(0.0);
                if (avgRating > 0) {
                  memberInfo.setRating(avgRating);
                }
              }
            } catch (Exception e) {
              logger.warn("Error calculating rating for user {}: {}", user.getId(), e.getMessage());
            }
            // Set avatar if exists
            if (user.getAvatar() != null) {
              RoomDetailsResponse.RoomMemberInfo.MemberAvatar avatar = new RoomDetailsResponse.RoomMemberInfo.MemberAvatar();
              avatar.setId(user.getAvatar().getId());
              memberInfo.setAvatar(avatar);
            }
            return memberInfo;
          })
          .collect(Collectors.toList());
      logger.info("Created members list with {} members", membersList.size());
    } else {
      logger.info("Room {} has no collaborators or collaborators is null", roomId);
    }
    response.setMembers(membersList);
    logger.info("Set members list to response, size: {}", response.getMembers() != null ? response.getMembers().size() : "null");
    
    return response;
  }

  public List<Room> getAllRooms(Integer offset, Integer limit) {
    int page = limit > 0 ? offset / limit : 0;
    Pageable pageable = PageRequest.of(
        page, limit, Sort.by("createdAt").descending());
    return roomRepository.findAll(pageable).getContent();
  }

  /**
   * Kick a user from a room (admin only)
   * Cannot kick the room creator
   */
  @Transactional
  public void kickUserFromRoom(Long roomId, Long userIdToKick, Long adminUserId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User admin = userRepository.findById(adminUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    
    User userToKick = userRepository.findById(userIdToKick)
        .orElseThrow(() -> new ResourceNotFoundException("User to kick not found"));
    
    // Check if admin is actually an admin
    if (!room.getAdmins().contains(admin)) {
      throw new ForbiddenException("Only administrators can kick users from the room");
    }
    
    // Cannot kick the room creator
    if (room.getCreatedBy() != null && room.getCreatedBy().getId().equals(userIdToKick)) {
      throw new BadRequestException("Cannot kick the room creator");
    }
    
    // Check if user is a member
    if (!room.getCollaborators().contains(userToKick)) {
      throw new BadRequestException("User is not a member of this room");
    }
    
    // Remove from collaborators
    room.getCollaborators().remove(userToKick);
    
    // Remove from admins if they are an admin (but not the creator)
    if (room.getAdmins().contains(userToKick)) {
      room.getAdmins().remove(userToKick);
    }
    
    roomRepository.save(room);
    
    // Create notification for kicked user
    try {
      String roomName = room.getName() != null ? room.getName() : "комнате";
      notificationService.createNotification(
        userIdToKick,
        "USER_KICKED",
        "Вас исключили из комнаты",
        String.format("Вас исключили из комнаты \"%s\"", roomName),
        String.format("{\"roomId\":%d}", roomId)
      );
    } catch (Exception e) {
      logger.error("Failed to create kick notification: {}", e.getMessage(), e);
    }
    
    logger.info("Admin {} kicked user {} from room {}", adminUserId, userIdToKick, roomId);
  }

  /**
   * Delete a message from room chat (admin only)
   */
  @Transactional
  public void deleteMessage(Long roomId, Long messageId, Long adminUserId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User admin = userRepository.findById(adminUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    
    // Check if message belongs to this room
    if (message.getRoom() == null || !message.getRoom().getId().equals(roomId)) {
      throw new BadRequestException("Message does not belong to this room");
    }
    
    // Check if admin is actually an admin
    if (!room.getAdmins().contains(admin)) {
      throw new ForbiddenException("Only administrators can delete messages");
    }
    
    // Mark message as deleted
    message.setIsDeleted(true);
    messageRepository.save(message);
    
    logger.info("Admin {} deleted message {} from room {}", adminUserId, messageId, roomId);
  }

  /**
   * Promote a user to admin (admin only)
   */
  @Transactional
  public void promoteToAdmin(Long roomId, Long userIdToPromote, Long adminUserId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    
    User admin = userRepository.findById(adminUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    
    User userToPromote = userRepository.findById(userIdToPromote)
        .orElseThrow(() -> new ResourceNotFoundException("User to promote not found"));
    
    // Check if admin is actually an admin
    if (!room.getAdmins().contains(admin)) {
      throw new ForbiddenException("Only administrators can promote users to admin");
    }
    
    // Check if user is a member
    if (!room.getCollaborators().contains(userToPromote)) {
      throw new BadRequestException("User must be a member of the room to become an admin");
    }
    
    // Check if already an admin
    if (room.getAdmins().contains(userToPromote)) {
      throw new ConflictException("User is already an admin");
    }
    
    // Add to admins
    room.getAdmins().add(userToPromote);
    roomRepository.save(room);
    
    // Create notification for promoted user
    try {
      String roomName = room.getName() != null ? room.getName() : "комнате";
      notificationService.createNotification(
        userIdToPromote,
        "USER_PROMOTED",
        "Вас назначили администратором",
        String.format("Вас назначили администратором комнаты \"%s\"", roomName),
        String.format("{\"roomId\":%d}", roomId)
      );
    } catch (Exception e) {
      logger.error("Failed to create promotion notification: {}", e.getMessage(), e);
    }
    
    logger.info("Admin {} promoted user {} to admin in room {}", adminUserId, userIdToPromote, roomId);
  }
}
