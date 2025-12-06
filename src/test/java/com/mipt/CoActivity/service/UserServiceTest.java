package com.mipt.CoActivity.service;

import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.model.UserSettings;
import com.mipt.CoActivity.model.RoomFolder;
import com.mipt.CoActivity.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomFolderRepository roomFolderRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ExternalLinkRepository externalLinkRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private InterestCategoryRepository interestCategoryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("user1", "user1@test.com", "password");
        user1.setId(1L);
        user1.setName("User 1");
        user1.setSubscriptions(new ArrayList<>());
        user1.setFollowers(new ArrayList<>());

        user2 = new User("user2", "user2@test.com", "password");
        user2.setId(2L);
        user2.setName("User 2");
        user2.setSubscriptions(new ArrayList<>());
        user2.setFollowers(new ArrayList<>());
    }

    @Test
    void testSubscribe_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.subscribe(1L, 2L);

        // Then
        verify(userRepository, times(2)).save(any(User.class));
        verify(notificationService, times(1)).createNotification(
            eq(2L),
            eq("FOLLOW"),
            eq("Новая подписка"),
            anyString(),
            anyString()
        );
    }

    @Test
    void testSubscribe_PreventsSelfSubscription() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userService.subscribe(1L, 1L);
        });

        assertEquals("Cannot subscribe to yourself", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never()).createNotification(
            anyLong(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    void testSubscribe_AlreadySubscribed_ReturnsSuccess() {
        // Given
        user1.getSubscriptions().add(user2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        userService.subscribe(1L, 2L);

        // Then
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testSubscribe_ThrowsExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.subscribe(1L, 2L);
        });
    }

    @Test
    void testUnsubscribe_Success() {
        // Given
        user1.getSubscriptions().add(user2);
        user2.getFollowers().add(user1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.unsubscribe(1L, 2L);

        // Then
        verify(userRepository, times(2)).save(any(User.class));
        assertFalse(user1.getSubscriptions().contains(user2));
        assertFalse(user2.getFollowers().contains(user1));
    }

    @Test
    void testUnsubscribe_NotSubscribed_ReturnsSuccess() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        userService.unsubscribe(1L, 2L);

        // Then
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("user1")).thenReturn(user1);

        // When
        User result = userService.getUserByUsername("user1");

        // Then
        assertNotNull(result);
        assertEquals(user1.getUsername(), result.getUsername());
    }

    @Test
    void testGetUserByEmail_Success() {
        // Given
        when(userRepository.findByEmail("user1@test.com")).thenReturn(user1);

        // When
        User result = userService.getUserByEmail("user1@test.com");

        // Then
        assertNotNull(result);
        assertEquals(user1.getEmail(), result.getEmail());
    }

    @Test
    void testIsUsernameExists_ReturnsTrue() {
        // Given
        when(userRepository.findByUsername("user1")).thenReturn(user1);

        // When
        boolean result = userService.isUsernameExists("user1");

        // Then
        assertTrue(result);
    }

    @Test
    void testIsUsernameExists_ReturnsFalse() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(null);

        // When
        boolean result = userService.isUsernameExists("nonexistent");

        // Then
        assertFalse(result);
    }

    @Test
    void testIsEmailExists_ReturnsTrue() {
        // Given
        when(userRepository.findByEmail("user1@test.com")).thenReturn(user1);

        // When
        boolean result = userService.isEmailExists("user1@test.com");

        // Then
        assertTrue(result);
    }

    @Test
    void testIsEmailExists_ReturnsFalse() {
        // Given
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(null);

        // When
        boolean result = userService.isEmailExists("nonexistent@test.com");

        // Then
        assertFalse(result);
    }

    @Test
    void testRegisterUser_Success() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(userRepository.findByEmail("newuser@test.com")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(roomRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        // When
        User result = userService.registerUser("newuser", "newuser@test.com", "password123");

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("newuser@test.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_ThrowsExceptionWhenUsernameExists() {
        // Given
        when(userRepository.findByUsername("existinguser")).thenReturn(user1);

        // When & Then
        assertThrows(com.mipt.CoActivity.exception.ConflictException.class, () -> {
            userService.registerUser("existinguser", "new@test.com", "password123");
        });
    }

    @Test
    void testRegisterUser_ThrowsExceptionWhenEmailExists() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(userRepository.findByEmail("existing@test.com")).thenReturn(user1);

        // When & Then
        assertThrows(com.mipt.CoActivity.exception.ConflictException.class, () -> {
            userService.registerUser("newuser", "existing@test.com", "password123");
        });
    }

    @Test
    void testRegisterUser_ThrowsExceptionWhenPasswordTooShort() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(userRepository.findByEmail("newuser@test.com")).thenReturn(null);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            userService.registerUser("newuser", "newuser@test.com", "short");
        });
    }

    @Test
    void testLoginUser_Success() {
        // Given
        when(userRepository.findByUsername("user1")).thenReturn(user1);
        when(passwordEncoder.matches("password", user1.getPasswordHash())).thenReturn(true);

        // When
        User result = userService.loginUser("user1", "password");

        // Then
        assertNotNull(result);
        assertEquals(user1.getUsername(), result.getUsername());
    }

    @Test
    void testLoginUser_WithEmail() {
        // Given
        when(userRepository.findByEmail("user1@test.com")).thenReturn(user1);
        when(passwordEncoder.matches("password", user1.getPasswordHash())).thenReturn(true);

        // When
        User result = userService.loginUser("user1@test.com", "password");

        // Then
        assertNotNull(result);
        assertEquals(user1.getEmail(), result.getEmail());
    }

    @Test
    void testLoginUser_ThrowsExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(null);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.loginUser("nonexistent", "password");
        });
    }

    @Test
    void testLoginUser_ThrowsExceptionWhenInvalidPassword() {
        // Given
        when(userRepository.findByUsername("user1")).thenReturn(user1);
        when(passwordEncoder.matches("wrongpassword", user1.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            userService.loginUser("user1", "wrongpassword");
        });
    }

    @Test
    void testGetUserProfile_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // When
        User result = userService.getUserProfile(1L);

        // Then
        assertNotNull(result);
        assertEquals(user1.getId(), result.getId());
    }

    @Test
    void testGetUserProfile_ThrowsExceptionWhenNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserProfile(999L);
        });
    }

    @Test
    void testGetUserRooms_Success() {
        // Given
        com.mipt.CoActivity.model.Room room1 = new com.mipt.CoActivity.model.Room();
        room1.setId(1L);
        com.mipt.CoActivity.model.Room room2 = new com.mipt.CoActivity.model.Room();
        room2.setId(2L);
        List<com.mipt.CoActivity.model.Room> rooms = new ArrayList<>();
        rooms.add(room1);
        rooms.add(room2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(roomRepository.findByCollaboratorsId(1L)).thenReturn(rooms);

        // When
        List<com.mipt.CoActivity.model.Room> result = userService.getUserRooms(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetPersonalInfo_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // When
        com.mipt.CoActivity.dto.PersonalInfoResponse result = userService.getPersonalInfo(1L);

        // Then
        assertNotNull(result);
        assertEquals(user1.getName(), result.getName());
        assertEquals(user1.getEmail(), result.getEmail());
    }

    @Test
    void testUpdateUserName_Success() {
        // Given
        com.mipt.CoActivity.dto.UpdateNameRequest request = new com.mipt.CoActivity.dto.UpdateNameRequest();
        request.setName("New Name");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.updateUserName(1L, request);

        // Then
        assertEquals("New Name", user1.getName());
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void testUpdateUserEmail_Success() {
        // Given
        com.mipt.CoActivity.dto.UpdateEmailRequest request = new com.mipt.CoActivity.dto.UpdateEmailRequest();
        request.setEmail("newemail@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("newemail@test.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.updateUserEmail(1L, request);

        // Then
        assertEquals("newemail@test.com", user1.getEmail());
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void testUpdateUserPhone_Success() {
        // Given
        com.mipt.CoActivity.dto.UpdatePhoneRequest request = new com.mipt.CoActivity.dto.UpdatePhoneRequest();
        request.setPhone("+1234567890");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.updateUserPhone(1L, request);

        // Then
        assertEquals("+1234567890", user1.getPhone());
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void testUpdateUserAddress_Success() {
        // Given
        com.mipt.CoActivity.dto.UpdateAddressRequest request = new com.mipt.CoActivity.dto.UpdateAddressRequest();
        request.setAddress("New Address");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.updateUserAddress(1L, request);

        // Then
        assertEquals("New Address", user1.getAddress());
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void testViewUserProfile_Success() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // When
        User result = userService.viewUserProfile(2L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(user2.getId(), result.getId());
    }

    @Test
    void testViewUserProfile_ThrowsExceptionWhenNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.viewUserProfile(999L, 1L);
        });
    }

    @Test
    void testGetUserSettings_Success() {
        // Given
        com.mipt.CoActivity.model.UserSettings settings = new com.mipt.CoActivity.model.UserSettings();
        settings.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(settings));

        // When
        com.mipt.CoActivity.model.UserSettings result = userService.getUserSettings(1L);

        // Then
        assertNotNull(result);
        assertEquals(settings.getId(), result.getId());
    }

    @Test
    void testLogoutUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.logoutUser(1L);

        // Then
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void testCalculateUserRating_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(roomRepository.findByCollaboratorsId(1L)).thenReturn(new ArrayList<>());

        // When
        Double result = userService.calculateUserRating(1L);

        // Then
        assertNotNull(result);
    }

    @Test
    void testPerformUserProfileAction_Success() {
        // Given
        UserProfileActionRequest request = new UserProfileActionRequest();
        request.setAction("add_friend");
        request.setTargetUserId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.performUserProfileAction(1L, request);

        // Then
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void testAcceptFriendRequest_Success() {
        // Given
        FriendRequestRequest request = new FriendRequestRequest();
        request.setTargetUserId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.acceptFriendRequest(1L, request);

        // Then
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void testDeclineFriendRequest_Success() {
        // Given
        FriendRequestRequest request = new FriendRequestRequest();
//        request.setFriendId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.declineFriendRequest(1L, request);

        // Then
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void testCreateRoomFolder_Success() {
        // Given
        CreateRoomFolderRequest request = new CreateRoomFolderRequest();
        request.setFolderName("My Folder");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(roomFolderRepository.save(any(RoomFolder.class))).thenAnswer(invocation -> {
            RoomFolder folder = invocation.getArgument(0);
            folder.setId(1L);
            return folder;
        });

        // When
        RoomFolder result = userService.createRoomFolder(1L, request);

        // Then
        assertNotNull(result);
        verify(roomFolderRepository, times(1)).save(any(RoomFolder.class));
    }

    @Test
    void testDeleteRoomFolder_Success() {
        // Given
        RoomFolder folder = new RoomFolder();
        folder.setId(1L);
        folder.setUser(user1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(roomFolderRepository.findById(1L)).thenReturn(Optional.of(folder));
        doNothing().when(roomFolderRepository).delete(any(RoomFolder.class));

        // When
        userService.deleteRoomFolder(1L, 1L);

        // Then
        verify(roomFolderRepository, times(1)).delete(folder);
    }

    @Test
    void testUpdateRoomFolder_Success() {
        // Given
        RoomFolder folder = new RoomFolder();
        folder.setId(1L);
        folder.setUser(user1);
        UpdateRoomFolderRequest request = new UpdateRoomFolderRequest();
        request.setFolderName("Updated Folder");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(roomFolderRepository.findById(1L)).thenReturn(Optional.of(folder));
        when(roomFolderRepository.save(any(RoomFolder.class))).thenReturn(folder);

        // When
        userService.updateRoomFolder(1L, 1L, request);

        // Then
        verify(roomFolderRepository, times(1)).save(folder);
    }

    @Test
    void testGetRoomDetails_Success() {
        // Given
        com.mipt.CoActivity.model.Room room = new com.mipt.CoActivity.model.Room();
        room.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        // When
        com.mipt.CoActivity.model.Room result = userService.getRoomDetails(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(room.getId(), result.getId());
    }

    @Test
    void testFilterRooms_Success() {
        // Given
        com.mipt.CoActivity.model.Room room1 = new com.mipt.CoActivity.model.Room();
        room1.setId(1L);
        List<com.mipt.CoActivity.model.Room> rooms = new ArrayList<>();
        rooms.add(room1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(roomRepository.findByCollaboratorsId(1L)).thenReturn(rooms);

        // When
        List<com.mipt.CoActivity.model.Room> result = userService.filterRooms(1L, "time", "asc");

        // Then
        assertNotNull(result);
    }

    @Test
    void testGetUserNotificationSettings_Success() {
        // Given
        UserSettings settings = new UserSettings();
        settings.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(settings));

        // When
        NotificationSettingsResponse result = userService.getUserNotificationSettings(1L);

        // Then
        assertNotNull(result);
    }

    @Test
    void testUpdateUserNotificationSettings_Success() {
        // Given
        UserSettings settings = new UserSettings();
        settings.setId(1L);
        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest();
//        request.setPushNotifications(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(settings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(settings);

        // When
        userService.updateUserNotificationSettings(1L, request);

        // Then
        verify(userSettingsRepository, times(1)).save(settings);
    }

    @Test
    void testGetUserExternalLinks_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(externalLinkRepository.findByUserId(1L)).thenReturn(new ArrayList<>());

        // When
        List<ExternalLinkResponse> result = userService.getUserExternalLinks(1L);

        // Then
        assertNotNull(result);
    }

    @Test
    void testUpdateInterests_Success() {
        // Given
        UpdateInterestsRequest request = new UpdateInterestsRequest();
        request.setInterests(new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.updateInterests(1L, request);

        // Then
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void testUpdateAbout_Success() {
        // Given
        UpdateAboutRequest request = new UpdateAboutRequest();
        request.setAbout("New about text");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        userService.updateAbout(1L, request);

        // Then
        assertEquals("New about text", user1.getAbout());
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void testGetAbout_Success() {
        // Given
        user1.setAbout("Test about");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // When
        String result = userService.getAbout(1L);

        // Then
        assertNotNull(result);
        assertEquals("Test about", result);
    }
}

