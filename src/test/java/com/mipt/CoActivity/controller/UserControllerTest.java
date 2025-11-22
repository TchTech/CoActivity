package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.BaseIntegrationTest;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomFolderRepository roomFolderRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        roomFolderRepository.deleteAll();
        roomRepository.deleteAll();
        userSettingsRepository.deleteAll();
        userRepository.deleteAll();

        testUser1 = new User("testuser1", "test1@example.com", passwordEncoder.encode("password123"));
        testUser1.setName("Test User 1");
        testUser1.setPhone("+1234567890");
        testUser1.setAddress("123 Test St");
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User("testuser2", "test2@example.com", passwordEncoder.encode("password123"));
        testUser2.setName("Test User 2");
        testUser2 = userRepository.save(testUser2);
    }

    @Test
    void shouldGetUserProfile() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{id}/profile", testUser1.getId())
            .then()
            .statusCode(200)
            .body("id", equalTo(testUser1.getId().intValue()))
            .body("name", equalTo("Test User 1"))
            .body("email", equalTo("test1@example.com"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{id}/profile", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    void shouldGetPersonalInfo() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{id}/profile/personal-info", testUser1.getId())
            .then()
            .statusCode(200)
            .body("name", equalTo("Test User 1"))
            .body("email", equalTo("test1@example.com"))
            .body("phone", equalTo("+1234567890"))
            .body("address", equalTo("123 Test St"));
    }

    @Test
    void shouldUpdateUserName() {
        Map<String, String> request = new HashMap<>();
        request.put("name", "Updated Name");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{id}/profile/personal-info/name", testUser1.getId())
            .then()
            .statusCode(200);

        // Verify the update
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{id}/profile/personal-info", testUser1.getId())
            .then()
            .statusCode(200)
            .body("name", equalTo("Updated Name"));
    }

    @Test
    void shouldUpdateUserEmail() {
        Map<String, String> request = new HashMap<>();
        request.put("email", "newemail@example.com");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{id}/profile/personal-info/email", testUser1.getId())
            .then()
            .statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{id}/profile/personal-info", testUser1.getId())
            .then()
            .statusCode(200)
            .body("email", equalTo("newemail@example.com"));
    }

    @Test
    void shouldUpdateUserPhone() {
        Map<String, String> request = new HashMap<>();
        request.put("phone", "+9876543210");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{id}/profile/personal-info/phone", testUser1.getId())
            .then()
            .statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{id}/profile/personal-info", testUser1.getId())
            .then()
            .statusCode(200)
            .body("phone", equalTo("+9876543210"));
    }

    @Test
    void shouldUpdateUserAddress() {
        Map<String, String> request = new HashMap<>();
        request.put("address", "456 New Address St");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{id}/profile/personal-info/address", testUser1.getId())
            .then()
            .statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{id}/profile/personal-info", testUser1.getId())
            .then()
            .statusCode(200)
            .body("address", equalTo("456 New Address St"));
    }

    @Test
    void shouldViewUserProfile() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("currentUserId", testUser1.getId())
            .when()
            .get("/users/{userId}/profile/view", testUser2.getId())
            .then()
            .statusCode(200)
            .body("id", equalTo(testUser2.getId().intValue()))
            .body("name", equalTo("Test User 2"));
    }

    @Test
    void shouldRegisterUser() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("username", "newuser")
            .formParam("email", "newuser@example.com")
            .formParam("password", "password123")
            .when()
            .post("/users/register")
            .then()
            .statusCode(201)
            .body("username", equalTo("newuser"))
            .body("email", equalTo("newuser@example.com"));
    }

    @Test
    void shouldReturn409WhenRegisteringDuplicateUsername() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("username", "duplicate")
            .formParam("email", "duplicate1@example.com")
            .formParam("password", "password123")
            .when()
            .post("/users/register")
            .then()
            .statusCode(201);

        given()
            .contentType(ContentType.URLENC)
            .formParam("username", "duplicate")
            .formParam("email", "duplicate2@example.com")
            .formParam("password", "password123")
            .when()
            .post("/users/register")
            .then()
            .statusCode(409);
    }

    @Test
    void shouldGetUserRooms() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{userId}/rooms", testUser1.getId())
            .then()
            .statusCode(200)
            .body(".", isA(List.class));
    }

    @Test
    void shouldGetUserSettings() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{userId}/settings", testUser1.getId())
            .then()
            .statusCode(200)
            .body("notificationsEnabled", notNullValue());
    }

    @Test
    void shouldGetUserNotificationSettings() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{userId}/settings/notifications", testUser1.getId())
            .then()
            .statusCode(200)
            .body("emailNotifications", notNullValue())
            .body("pushNotifications", notNullValue());
    }

    @Test
    void shouldUpdateGeneralNotificationSettings() {
        Map<String, Boolean> request = new HashMap<>();
        request.put("emailNotifications", false);
        request.put("pushNotifications", true);

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{userId}/settings/general-notifications", testUser1.getId())
            .then()
            .statusCode(200)
            .body("emailNotifications", equalTo(false))
            .body("pushNotifications", equalTo(true));
    }

    @Test
    void shouldCreateRoomFolder() {
        Map<String, String> request = new HashMap<>();
        request.put("folderName", "My Folder");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/users/{userId}/rooms/folder", testUser1.getId())
            .then()
            .statusCode(201)
            .body("folderName", equalTo("My Folder"));
    }

    @Test
    void shouldUpdateRoomFolder() {
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("folderName", "Original Folder");

        Long folderId = ((Number) given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/users/{userId}/rooms/folder", testUser1.getId())
            .then()
            .statusCode(201)
            .extract()
            .path("id")).longValue();

        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("folderName", "Updated Folder");

        given()
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .when()
            .put("/users/{userId}/rooms/folder/{folderId}/update", testUser1.getId(), folderId)
            .then()
            .statusCode(200);
    }

    @Test
    void shouldDeleteRoomFolder() {
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("folderName", "Folder to Delete");

        Long folderId = ((Number) given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/users/{userId}/rooms/folder", testUser1.getId())
            .then()
            .statusCode(201)
            .extract()
            .path("id")).longValue();

        given()
            .contentType(ContentType.JSON)
            .when()
            .delete("/users/{userId}/rooms/folder/{folderId}/delete", testUser1.getId(), folderId)
            .then()
            .statusCode(200);
    }

    @Test
    void shouldSubscribeUser() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", testUser1.getId())
            .formParam("userToSubscribeId", testUser2.getId())
            .when()
            .post("/users/subscribe")
            .then()
            .statusCode(200);
    }

    @Test
    void shouldReturn400WhenUpdatingNameWithEmptyValue() {
        Map<String, String> request = new HashMap<>();
        request.put("name", "");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{id}/profile/personal-info/name", testUser1.getId())
            .then()
            .statusCode(400);
    }

    @Test
    void shouldGetPrivacyAndRecommendations() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/users/{userId}/settings/privacy-and-recommendations", testUser1.getId())
            .then()
            .statusCode(200)
            .body("roomRecommendations", notNullValue());
    }

    @Test
    void shouldUpdateRoomRecommendations() {
        Map<String, Boolean> request = new HashMap<>();
        request.put("roomRecommendations", false);

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{userId}/settings/room-recommendations", testUser1.getId())
            .then()
            .statusCode(200)
            .body("roomRecommendations", equalTo(false));
    }

    @Test
    void shouldUpdateFriendsDataAccess() {
        Map<String, String> request = new HashMap<>();
        request.put("friendsAccess", "specific");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{userId}/settings/friends-data-access", testUser1.getId())
            .then()
            .statusCode(200)
            .body("friendsAccess", equalTo("specific"));
    }

    @Test
    void shouldUpdateDataLinksAccess() {
        Map<String, String> request = new HashMap<>();
        request.put("dataLinksAccess", "friends");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/users/{userId}/settings/data-links-access", testUser1.getId())
            .then()
            .statusCode(200)
            .body("dataLinksAccess", equalTo("friends"));
    }

    @Test
    void shouldPerformUserProfileActionAddFriend() {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "addFriend");
        request.put("targetUserId", testUser2.getId());

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/users/{userId}/profile/view", testUser1.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldGetCommonRooms() {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "getCommonRooms");
        request.put("targetUserId", testUser2.getId());

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/users/{userId}/profile/view", testUser1.getId())
            .then()
            .statusCode(200)
            .body("commonRooms", notNullValue());
    }

    @Test
    void shouldAcceptFriendRequest() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", testUser2.getId())
            .formParam("userToSubscribeId", testUser1.getId())
            .when()
            .post("/users/subscribe")
            .then()
            .statusCode(200);

        Map<String, Long> request = new HashMap<>();
        request.put("targetUserId", testUser2.getId());

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/users/{userId}/profile/view/friend-request", testUser1.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldDeclineFriendRequest() {
        Map<String, Long> request = new HashMap<>();
        request.put("targetUserId", testUser2.getId());

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/users/{userId}/profile/view/friend-request/decline", testUser1.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldFilterRoomsByTime() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("filterBy", "time")
            .queryParam("order", "ascending")
            .when()
            .get("/users/{userId}/rooms/filter", testUser1.getId())
            .then()
            .statusCode(200)
            .body(".", isA(List.class));
    }

    @Test
    void shouldFilterRoomsByImportance() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("filterBy", "importance")
            .queryParam("order", "descending")
            .when()
            .get("/users/{userId}/rooms/filter", testUser1.getId())
            .then()
            .statusCode(200)
            .body(".", isA(List.class));
    }
}

