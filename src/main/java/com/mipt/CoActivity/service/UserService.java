package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.exception.*;
import com.mipt.CoActivity.model.*;
import com.mipt.CoActivity.repository.*;
import com.mipt.CoActivity.repository.ExternalLinkRepository;
import com.mipt.CoActivity.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
  private final ExternalLinkRepository externalLinkRepository;
  private final ImageService imageService;

  @Autowired
  UserService(UserRepository userRepository,
              UserSettingsRepository userSettingsRepository,
              RoomRepository roomRepository,
              RoomFolderRepository roomFolderRepository,
              BCryptPasswordEncoder passwordEncoder,
              ExternalLinkRepository externalLinkRepository,
              ImageService imageService) {
    this.userRepository = userRepository;
    this.userSettingsRepository = userSettingsRepository;
    this.roomRepository = roomRepository;
    this.roomFolderRepository = roomFolderRepository;
    this.passwordEncoder = passwordEncoder;
    this.externalLinkRepository = externalLinkRepository;
    this.imageService = imageService;
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
    logger.info("Registering user - username: {}, email: {}", username, email);
    
    // Валидация входных данных
    if (username == null || username.trim().isEmpty()) {
      throw new BadRequestException("Username cannot be empty");
    }
    if (email == null || email.trim().isEmpty()) {
      throw new BadRequestException("Email cannot be empty");
    }
    if (password == null || password.isEmpty()) {
      throw new BadRequestException("Password cannot be empty");
    }
    if (password.length() < 8) {
      throw new BadRequestException("Password must be at least 8 characters long");
    }
    
    if (isUsernameExists(username)) {
      throw new ConflictException("Username already exists");
    }

    if (isEmailExists(email)) {
      throw new ConflictException("Email already exists");
    }

    try {
      String hashedPassword = hashPassword(password);
      User newUser = new User(username, email, hashedPassword);
      User savedUser = userRepository.save(newUser);
      
      // Auto-join default room
      Room defaultRoom = roomRepository.findByIsDefaultTrue().orElse(null);
      if (defaultRoom != null) {
        defaultRoom.getCollaborators().add(savedUser);
        roomRepository.save(defaultRoom);
        logger.info("User {} auto-joined default room {}", savedUser.getId(), defaultRoom.getId());
      }
      
      logger.info("User registered successfully - id: {}, username: {}", savedUser.getId(), savedUser.getUsername());
      return savedUser;
    } catch (Exception e) {
      logger.error("Error registering user: ", e);
      throw new RuntimeException("Failed to register user: " + e.getMessage(), e);
    }
  }

  /**
   * Аутентификация пользователя по логину (username или email) и паролю
   * @param login - username или email
   * @param password - пароль в открытом виде
   * @return User - объект пользователя при успешной аутентификации
   * @throws ResourceNotFoundException - если пользователь не найден
   * @throws BadRequestException - если пароль неверный
   */
  public User loginUser(String login, String password) {
    if (login == null || login.trim().isEmpty()) {
      throw new BadRequestException("Login cannot be empty");
    }
    if (password == null || password.isEmpty()) {
      throw new BadRequestException("Password cannot be empty");
    }

    // Определяем, является ли login email или username
    User user = null;
    if (login.contains("@")) {
      user = getUserByEmail(login);
    } else {
      user = getUserByUsername(login);
    }

    if (user == null) {
      throw new ResourceNotFoundException("User not found");
    }

    // Проверяем пароль
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BadRequestException("Invalid password");
    }

    logger.info("User {} successfully logged in", user.getUsername());
    return user;
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

  public NotificationSettingsResponse getUserNotificationSettings(Long userId) {
    UserSettings settings = getUserSettings(userId);
    return NotificationSettingsResponse.builder()
        .roomInvitationNotifications(settings.getFriendRequestNotifications())
        .messageNotifications(settings.getMessageNotifications())
        .roomRemovalNotifications(settings.getEventNotifications())
        .emailNotifications(settings.getEmailNotifications())
        .pushNotifications(settings.getPushNotifications())
        .build();
  }

  @Transactional
  public void updateUserNotificationSettings(Long userId, UpdateNotificationSettingsRequest request) {
    UserSettings settings = getUserSettings(userId);
    if (request.getRoomInvitationNotifications() != null) {
      settings.setFriendRequestNotifications(request.getRoomInvitationNotifications());
    }
    if (request.getMessageNotifications() != null) {
      settings.setMessageNotifications(request.getMessageNotifications());
    }
    if (request.getRoomRemovalNotifications() != null) {
      settings.setEventNotifications(request.getRoomRemovalNotifications());
    }
    userSettingsRepository.save(settings);
  }

  @Transactional
  public void logoutUser(Long id) {
    getUserProfile(id);
    logger.info("User {} logged out", id);
  }

  public List<ExternalLinkResponse> getUserExternalLinks(Long id) {
    getUserProfile(id);
    List<ExternalLink> links = externalLinkRepository.findByUserId(id);
    return links.stream()
        .map(link -> ExternalLinkResponse.builder()
            .id(link.getId())
            .platformName(link.getPlatformName())
            .label(link.getLabel())
            .url(link.getUrl())
            .build())
        .collect(Collectors.toList());
  }

  @Transactional
  public ExternalLinkResponse addExternalLink(Long id, ExternalLinkRequest request) {
    User user = getUserProfile(id);
    
    if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
      throw new BadRequestException("URL cannot be empty");
    }
    
    // Check limit (max 10 links per user)
    List<ExternalLink> existingLinks = externalLinkRepository.findByUserId(id);
    if (existingLinks.size() >= 10) {
      throw new BadRequestException("Maximum 10 external links allowed per user");
    }
    
    // Validate and normalize URL
    String url = request.getUrl().trim();
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "https://" + url;
    }
    
    // Validate URL format
    try {
      new java.net.URL(url);
    } catch (java.net.MalformedURLException e) {
      throw new BadRequestException("Invalid URL format");
    }

    ExternalLink link = new ExternalLink(user, request.getPlatformName(), url);
    if (request.getLabel() != null) {
      link.setLabel(request.getLabel());
    }
    ExternalLink savedLink = externalLinkRepository.save(link);
    
    return ExternalLinkResponse.builder()
        .id(savedLink.getId())
        .platformName(savedLink.getPlatformName())
        .label(savedLink.getLabel())
        .url(savedLink.getUrl())
        .build();
  }

  @Transactional
  public ExternalLinkResponse updateExternalLink(Long id, Long linkId, ExternalLinkRequest request) {
    getUserProfile(id);
    ExternalLink link = externalLinkRepository.findById(linkId)
        .orElseThrow(() -> new ResourceNotFoundException("External link not found"));

    if (!link.getUser().getId().equals(id)) {
      throw new ForbiddenException("You can only update your own external links");
    }

    // Validate URL if provided
    if (request.getUrl() != null && !request.getUrl().trim().isEmpty()) {
      String url = request.getUrl().trim();
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "https://" + url;
      }
      
      try {
        new java.net.URL(url);
      } catch (java.net.MalformedURLException e) {
        throw new BadRequestException("Invalid URL format");
      }
      
      link.setUrl(url);
    }

    if (request.getPlatformName() != null) {
      link.setPlatformName(request.getPlatformName());
    }

    if (request.getLabel() != null) {
      link.setLabel(request.getLabel());
    }

    link.setUpdatedAt(java.time.Instant.now());
    ExternalLink savedLink = externalLinkRepository.save(link);

    return ExternalLinkResponse.builder()
        .id(savedLink.getId())
        .platformName(savedLink.getPlatformName())
        .label(savedLink.getLabel())
        .url(savedLink.getUrl())
        .build();
  }

  @Transactional
  public void deleteExternalLink(Long id, Long linkId) {
    getUserProfile(id);
    ExternalLink link = externalLinkRepository.findById(linkId)
        .orElseThrow(() -> new ResourceNotFoundException("External link not found"));
    
    if (!link.getUser().getId().equals(id)) {
      throw new ForbiddenException("You don't have permission to delete this link");
    }
    
    externalLinkRepository.delete(link);
  }

  public Integer getRoomCount(Long userId) {
    getUserProfile(userId);
    // Count rooms where user is a member (collaborator)
    List<Room> rooms = roomRepository.findByCollaboratorsId(userId);
    return rooms != null ? rooms.size() : 0;
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

  /**
   * Рассчитывает средний рейтинг пользователя на основе отзывов
   * @param userId - ID пользователя
   * @return Double - средний рейтинг от 0 до 5, или null если нет отзывов
   */
  public Double calculateUserRating(Long userId) {
    User user = getUserProfile(userId);
    List<Feedback> feedbacks = user.getFeedbacks();
    
    if (feedbacks == null || feedbacks.isEmpty()) {
      return null;
    }
    
    double sum = feedbacks.stream()
            .filter(f -> f.getRating() != null)
            .mapToDouble(Feedback::getRating)
            .sum();
    
    long count = feedbacks.stream()
            .filter(f -> f.getRating() != null)
            .count();
    
    if (count == 0) {
      return null;
    }
    
    return sum / count;
  }

  @Transactional
  public User uploadAvatar(Long userId, MultipartFile file) throws IOException {
    User user = getUserProfile(userId);
    
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("File cannot be empty");
    }

    // Validate file size (max 10MB)
    if (file.getSize() > 10 * 1024 * 1024) {
      throw new BadRequestException("File size cannot exceed 10MB");
    }

    // Validate file type
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new BadRequestException("File must be an image");
    }

    com.mipt.CoActivity.model.Image avatarImage = imageService.uploadImage(file);
    user.setAvatar(avatarImage);
    return userRepository.save(user);
  }
}
