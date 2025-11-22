package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.exception.*;
import com.mipt.CoActivity.model.*;
import com.mipt.CoActivity.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
  private static final Logger logger = LoggerFactory.getLogger(UserService.class);
  private final UserRepository userRepository;
  private final UserSettingsRepository userSettingsRepository;
  private final RoomRepository roomRepository;
  private final RoomFolderRepository roomFolderRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Autowired
  UserService(UserRepository userRepository,
              UserSettingsRepository userSettingsRepository,
              RoomRepository roomRepository,
              RoomFolderRepository roomFolderRepository,
              BCryptPasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.userSettingsRepository = userSettingsRepository;
    this.roomRepository = roomRepository;
    this.roomFolderRepository = roomFolderRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User getUserByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public User getUserByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public boolean isUsernameExists(String username) {
    return userRepository.findByUsername(username) != null;
  }

  public boolean isEmailExists(String email) {
    return userRepository.findByEmail(email) != null;
  }

  private String hashPassword(String password) {
    return passwordEncoder.encode(password);
  }

  public User registerUser(String username, String email, String password) {
    if (isUsernameExists(username)) {
      throw new ConflictException("Username already exists");
    }

    if (isEmailExists(email)) {
      throw new ConflictException("Email already exists");
    }

    String hashedPassword = hashPassword(password);

    User newUser = new User(username, email, hashedPassword);

    return userRepository.save(newUser);
  }

  public void subscribe(Long userId, Long userToSubscribeId) {
    User user =
            userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    User userToSubscribe =
            userRepository
                    .findById(userToSubscribeId)
                    .orElseThrow(() -> new ResourceNotFoundException("User to subscribe not found"));

    if (user.getSubscriptions().contains(userToSubscribe)) {
      throw new ConflictException("User is already subscribed to this user");
    }

    user.getSubscriptions().add(userToSubscribe);
    userToSubscribe.getFollowers().add(user);

    userRepository.save(user);
    userRepository.save(userToSubscribe);
  }

  public void unsubscribe(Long userId, Long userToUnsubscribeId) {
    User user =
            userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    User userToUnsubscribe =
            userRepository
                    .findById(userToUnsubscribeId)
                    .orElseThrow(() -> new ResourceNotFoundException("User to unsubscribe not found"));

    if (!user.getSubscriptions().contains(userToUnsubscribe)) {
      throw new ConflictException("User is not subscribed to this user");
    }

    user.getSubscriptions().remove(userToUnsubscribe);
    userToUnsubscribe.getFollowers().remove(user);

    userRepository.save(user);
    userRepository.save(userToUnsubscribe);
  }

  public User getUserProfile(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  public PersonalInfoResponse getPersonalInfo(Long id) {
    User user = getUserProfile(id);
    PersonalInfoResponse response = new PersonalInfoResponse();
    response.setName(user.getName());
    response.setEmail(user.getEmail());
    response.setPhone(user.getPhone());
    response.setAddress(user.getAddress());
    return response;
  }

  @Transactional
  public void updateUserName(Long id, UpdateNameRequest request) {
    User user = getUserProfile(id);
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new BadRequestException("Name cannot be empty");
    }
    user.setName(request.getName());
    userRepository.save(user);
  }

  @Transactional
  public void updateUserEmail(Long id, UpdateEmailRequest request) {
    User user = getUserProfile(id);
    if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
      throw new BadRequestException("Email cannot be empty");
    }
    if (isEmailExists(request.getEmail()) && !user.getEmail().equals(request.getEmail())) {
      throw new ConflictException("Email already exists");
    }
    user.setEmail(request.getEmail());
    userRepository.save(user);
  }

  @Transactional
  public void updateUserPhone(Long id, UpdatePhoneRequest request) {
    User user = getUserProfile(id);
    user.setPhone(request.getPhone());
    userRepository.save(user);
  }

  @Transactional
  public void updateUserAddress(Long id, UpdateAddressRequest request) {
    User user = getUserProfile(id);
    user.setAddress(request.getAddress());
    userRepository.save(user);
  }

  public User viewUserProfile(Long userId, Long currentUserId) {
    User targetUser = getUserProfile(userId);
    User currentUser = getUserProfile(currentUserId);

    UserSettings settings = userSettingsRepository.findByUser(targetUser)
            .orElseGet(() -> {
              UserSettings newSettings = new UserSettings(targetUser);
              return userSettingsRepository.save(newSettings);
            });

    if ("private".equals(settings.getProfileVisibility())) {
      throw new ForbiddenException("Profile is private");
    }
    if ("friends".equals(settings.getProfileVisibility())) {
      if (!currentUser.getSubscriptions().contains(targetUser)) {
        throw new ForbiddenException("Profile is only visible to friends");
      }
    }
    logger.debug("User {} viewed profile of user {}", currentUserId, userId);
    return targetUser;
  }

  @Transactional
  public void performUserProfileAction(Long userId, UserProfileActionRequest request) {
    User user = getUserProfile(userId);
    User targetUser = getUserProfile(request.getTargetUserId());

    switch (request.getAction()) {
      case "block":
        logger.info("User {} (id: {}) blocked user {} (id: {})",
                user.getName(), userId, targetUser.getName(), request.getTargetUserId());
        break;
      case "addFriend":
        if (user.getSubscriptions().contains(targetUser)) {
          throw new ConflictException("User is already a friend");
        }
        subscribe(userId, request.getTargetUserId());
        break;
      case "getCommonRooms":
        break;
      default:
        throw new BadRequestException("Invalid action: " + request.getAction());
    }
  }

  @Transactional
  public void acceptFriendRequest(Long userId, FriendRequestRequest request) {
    User user = getUserProfile(userId);
    User requester = getUserProfile(request.getTargetUserId());

    if (user.getSubscriptions().contains(requester)) {
      throw new ConflictException("Friend request already accepted");
    }

    subscribe(userId, request.getTargetUserId());
  }

  @Transactional
  public void declineFriendRequest(Long userId, FriendRequestRequest request) {

    logger.info("User {} declined friend request from {}", userId, request.getTargetUserId());
  }

  public List<Room> getUserRooms(Long userId) {
    getUserProfile(userId);
    return roomRepository.findByCollaboratorsId(userId);
  }

  @Transactional
  public RoomFolder createRoomFolder(Long userId, CreateRoomFolderRequest request) {
    User user = getUserProfile(userId);
    if (request.getFolderName() == null || request.getFolderName().trim().isEmpty()) {
      throw new BadRequestException("Folder name cannot be empty");
    }
    RoomFolder folder = new RoomFolder(request.getFolderName(), user);
    return roomFolderRepository.save(folder);
  }

  @Transactional
  public void deleteRoomFolder(Long userId, Long folderId) {
    RoomFolder folder = roomFolderRepository.findById(folderId)
            .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

    if (!folder.getUser().getId().equals(userId)) {
      throw new ForbiddenException("You don't have permission to delete this folder");
    }

    if (!folder.getRooms().isEmpty()) {
      throw new BadRequestException("Cannot delete folder with rooms");
    }

    roomFolderRepository.delete(folder);
  }

  @Transactional
  public void updateRoomFolder(Long userId, Long folderId, UpdateRoomFolderRequest request) {
    RoomFolder folder = roomFolderRepository.findById(folderId)
            .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

    if (!folder.getUser().getId().equals(userId)) {
      throw new ForbiddenException("You don't have permission to update this folder");
    }

    if (request.getFolderName() == null || request.getFolderName().trim().isEmpty()) {
      throw new BadRequestException("Folder name cannot be empty");
    }

    folder.setFolderName(request.getFolderName());
    roomFolderRepository.save(folder);
  }

  public Room getRoomDetails(Long userId, Long roomId) {
    User user = getUserProfile(userId);
    Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    if (!room.getCollaborators().contains(user)) {
      throw new ForbiddenException("You don't have access to this room");
    }

    return room;
  }

  public List<Room> filterRooms(Long userId, String filterBy, String order) {
    List<Room> rooms = getUserRooms(userId);

    if ("time".equals(filterBy)) {
      rooms.sort((r1, r2) -> {
        int comparison = r1.getMeetingTime().compareTo(r2.getMeetingTime());
        return "descending".equals(order) ? -comparison : comparison;
      });
    } else if ("importance".equals(filterBy)) {
      rooms.sort((r1, r2) -> {
        int comparison = Integer.compare(
                r1.getCollaborators().size(),
                r2.getCollaborators().size()
        );
        return "descending".equals(order) ? -comparison : comparison;
      });
    }

    return rooms;
  }

  public UserSettings getUserSettings(Long userId) {
    User user = getUserProfile(userId);
    return userSettingsRepository.findByUser(user)
            .orElseGet(() -> {
              UserSettings settings = new UserSettings(user);
              return userSettingsRepository.save(settings);
            });
  }

  public UserSettings getUserNotificationSettings(Long userId) {
    return getUserSettings(userId);
  }

  @Transactional
  public UserSettings updateGeneralNotificationSettings(Long userId, GeneralNotificationSettingsRequest request) {
    UserSettings settings = getUserSettings(userId);
    if (request.getEmailNotifications() != null) {
      settings.setEmailNotifications(request.getEmailNotifications());
    }
    if (request.getPushNotifications() != null) {
      settings.setPushNotifications(request.getPushNotifications());
    }
    return userSettingsRepository.save(settings);
  }

  public UserSettings getPrivacyAndRecommendations(Long userId) {
    return getUserSettings(userId);
  }

  @Transactional
  public UserSettings updateRoomRecommendations(Long userId, RoomRecommendationsRequest request) {
    UserSettings settings = getUserSettings(userId);
    if (request.getRoomRecommendations() != null) {
      settings.setRoomRecommendations(request.getRoomRecommendations());
    }
    return userSettingsRepository.save(settings);
  }

  @Transactional
  public UserSettings updateFriendsDataAccess(Long userId, FriendsDataAccessRequest request) {
    UserSettings settings = getUserSettings(userId);
    if (request.getFriendsAccess() != null) {
      if (!List.of("all", "specific", "none").contains(request.getFriendsAccess())) {
        throw new BadRequestException("Invalid friendsAccess value");
      }
      settings.setFriendsAccess(request.getFriendsAccess());
    }
    return userSettingsRepository.save(settings);
  }

  @Transactional
  public UserSettings updateDataLinksAccess(Long userId, DataLinksAccessRequest request) {
    UserSettings settings = getUserSettings(userId);
    if (request.getDataLinksAccess() != null) {
      if (!List.of("public", "friends", "private").contains(request.getDataLinksAccess())) {
        throw new BadRequestException("Invalid dataLinksAccess value");
      }
      settings.setDataLinksAccess(request.getDataLinksAccess());
    }
    return userSettingsRepository.save(settings);
  }

  public List<Room> getCommonRooms(Long userId, Long targetUserId) {
    getUserProfile(userId);
    getUserProfile(targetUserId);

    List<Room> userRooms = roomRepository.findByCollaboratorsId(userId);
    List<Room> targetRooms = roomRepository.findByCollaboratorsId(targetUserId);

    return userRooms.stream()
            .filter(targetRooms::contains)
            .collect(Collectors.toList());
  }
}
