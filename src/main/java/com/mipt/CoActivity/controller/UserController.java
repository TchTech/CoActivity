package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.model.UserSettings;
import com.mipt.CoActivity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class UserController {
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);
  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/{id}/profile")
  public ResponseEntity<User> getUserProfile(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserProfile(id));
  }

  @GetMapping("/{id}/rating")
  public ResponseEntity<Map<String, Object>> getUserRating(@PathVariable Long id) {
    Double rating = userService.calculateUserRating(id);
    return ResponseEntity.ok(Map.of("rating", rating != null ? rating : 0.0, "hasRating", rating != null));
  }

  @GetMapping("/{id}/profile/personal-info")
  public ResponseEntity<PersonalInfoResponse> getPersonalInfo(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getPersonalInfo(id));
  }

  @PutMapping("/{id}/profile/personal-info/name")
  public ResponseEntity<Void> updateUserName(
      @PathVariable Long id, @RequestBody UpdateNameRequest request) {
    userService.updateUserName(id, request);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{id}/profile/personal-info/email")
  public ResponseEntity<Void> updateUserEmail(
      @PathVariable Long id, @RequestBody UpdateEmailRequest request) {
    userService.updateUserEmail(id, request);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{id}/profile/personal-info/phone")
  public ResponseEntity<Void> updateUserPhone(
      @PathVariable Long id, @RequestBody UpdatePhoneRequest request) {
    userService.updateUserPhone(id, request);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{id}/profile/personal-info/address")
  public ResponseEntity<Void> updateUserAddress(
      @PathVariable Long id, @RequestBody UpdateAddressRequest request) {
    userService.updateUserAddress(id, request);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{userId}/profile/view")
  public ResponseEntity<User> viewUserProfile(
      @PathVariable Long userId, @RequestParam Long currentUserId) {
    return ResponseEntity.ok(userService.viewUserProfile(userId, currentUserId));
  }

  @PostMapping("/{userId}/profile/view")
  public ResponseEntity<?> userProfileActions(
      @PathVariable Long userId, @RequestBody UserProfileActionRequest request) {
    if ("getCommonRooms".equals(request.getAction())) {
      List<Room> commonRooms = userService.getCommonRooms(userId, request.getTargetUserId());
      return ResponseEntity.ok(Map.of("commonRooms", commonRooms));
    }
    userService.performUserProfileAction(userId, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{userId}/profile/view/friend-request")
  public ResponseEntity<Void> acceptFriendRequest(
      @PathVariable Long userId, @RequestBody FriendRequestRequest request) {
    userService.acceptFriendRequest(userId, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{userId}/profile/view/friend-request/decline")
  public ResponseEntity<Void> declineFriendRequest(
      @PathVariable Long userId, @RequestBody FriendRequestRequest request) {
    userService.declineFriendRequest(userId, request);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{userId}/rooms")
  public ResponseEntity<List<Room>> getUserRooms(@PathVariable Long userId) {
    return ResponseEntity.ok(userService.getUserRooms(userId));
  }

  @PostMapping("/{userId}/rooms/folder")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<com.mipt.CoActivity.model.RoomFolder> createRoomFolder(
      @PathVariable Long userId, @RequestBody CreateRoomFolderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.createRoomFolder(userId, request));
  }

  @DeleteMapping("/{userId}/rooms/folder/{folderId}/delete")
  public ResponseEntity<Void> deleteRoomFolder(
      @PathVariable Long userId, @PathVariable Long folderId) {
    userService.deleteRoomFolder(userId, folderId);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{userId}/rooms/folder/{folderId}/update")
  public ResponseEntity<Void> updateRoomFolder(
      @PathVariable Long userId,
      @PathVariable Long folderId,
      @RequestBody UpdateRoomFolderRequest request) {
    userService.updateRoomFolder(userId, folderId, request);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{userId}/rooms/{roomId}")
  public ResponseEntity<Room> getRoomDetails(
      @PathVariable Long userId, @PathVariable Long roomId) {
    return ResponseEntity.ok(userService.getRoomDetails(userId, roomId));
  }

  @GetMapping("/{userId}/rooms/filter")
  public ResponseEntity<List<Room>> filterRooms(
      @PathVariable Long userId,
      @RequestParam String filterBy,
      @RequestParam(required = false, defaultValue = "ascending") String order) {
    return ResponseEntity.ok(userService.filterRooms(userId, filterBy, order));
  }

  @GetMapping("/{userId}/settings")
  public ResponseEntity<UserSettings> getUserSettings(@PathVariable Long userId) {
    return ResponseEntity.ok(userService.getUserSettings(userId));
  }

  @GetMapping("/{userId}/settings/notifications")
  public ResponseEntity<NotificationSettingsResponse> getUserNotificationSettings(@PathVariable Long userId) {
    return ResponseEntity.ok(userService.getUserNotificationSettings(userId));
  }

  @PutMapping("/{userId}/settings/notifications")
  public ResponseEntity<Void> updateUserNotificationSettings(
      @PathVariable Long userId, @RequestBody UpdateNotificationSettingsRequest request) {
    userService.updateUserNotificationSettings(userId, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/profile/logout")
  public ResponseEntity<Void> changeUserProfile(@PathVariable Long id) {
    userService.logoutUser(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/profile/external-links")
  public ResponseEntity<List<ExternalLinkResponse>> getUserExternalLinks(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserExternalLinks(id));
  }

  @PostMapping("/{id}/profile/external-links")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<ExternalLinkResponse> addExternalLink(
      @PathVariable Long id, @RequestBody ExternalLinkRequest request) {
    ExternalLinkResponse response = userService.addExternalLink(id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @DeleteMapping("/{id}/profile/external-links/{linkId}")
  public ResponseEntity<Void> deleteExternalLink(
      @PathVariable Long id, @PathVariable Long linkId) {
    userService.deleteExternalLink(id, linkId);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{userId}/settings/general-notifications")
  public ResponseEntity<UserSettings> getGeneralNotificationSettings(@PathVariable Long userId) {
    UserSettings settings = userService.getUserSettings(userId);
    return ResponseEntity.ok(settings);
  }

  @PutMapping("/{userId}/settings/general-notifications")
  public ResponseEntity<UserSettings> updateGeneralNotificationSettings(
      @PathVariable Long userId, @RequestBody GeneralNotificationSettingsRequest request) {
    return ResponseEntity.ok(userService.updateGeneralNotificationSettings(userId, request));
  }

  @GetMapping("/{userId}/settings/privacy-and-recommendations")
  public ResponseEntity<UserSettings> getPrivacyAndRecommendations(@PathVariable Long userId) {
    return ResponseEntity.ok(userService.getPrivacyAndRecommendations(userId));
  }

  @PutMapping("/{userId}/settings/room-recommendations")
  public ResponseEntity<UserSettings> updateRoomRecommendations(
      @PathVariable Long userId, @RequestBody RoomRecommendationsRequest request) {
    return ResponseEntity.ok(userService.updateRoomRecommendations(userId, request));
  }

  @PutMapping("/{userId}/settings/friends-data-access")
  public ResponseEntity<UserSettings> updateFriendsDataAccess(
      @PathVariable Long userId, @RequestBody FriendsDataAccessRequest request) {
    return ResponseEntity.ok(userService.updateFriendsDataAccess(userId, request));
  }

  @PutMapping("/{userId}/settings/data-links-access")
  public ResponseEntity<UserSettings> updateDataLinksAccess(
      @PathVariable Long userId, @RequestBody DataLinksAccessRequest request) {
    return ResponseEntity.ok(userService.updateDataLinksAccess(userId, request));
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<User> registerUser(
      @RequestParam String username, @RequestParam String email, @RequestParam String password) {
    try {
      User user = userService.registerUser(username, email, password);
      return ResponseEntity.status(HttpStatus.CREATED).body(user);
    } catch (Exception e) {
      // Логируем ошибку для отладки
      logger.error("Error in registerUser endpoint: ", e);
      throw e; // Пробрасываем дальше для обработки GlobalExceptionHandler
    }
  }

  @PostMapping("/login")
  public ResponseEntity<User> loginUser(
      @RequestParam String login, @RequestParam String password) {
    User user = userService.loginUser(login, password);
    return ResponseEntity.ok(user);
  }

  @PostMapping("/subscribe")
  public void subscribe(@RequestParam Long userId, @RequestParam Long userToSubscribeId) {
    userService.subscribe(userId, userToSubscribeId);
  }
}
