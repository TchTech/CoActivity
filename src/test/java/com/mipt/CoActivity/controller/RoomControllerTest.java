package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.BaseIntegrationTest;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class RoomControllerTest extends BaseIntegrationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RoomNotificationSettingsRepository roomNotificationSettingsRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser;
    private Room testRoom;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        messageRepository.deleteAll();
        roomNotificationSettingsRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("roomuser", "roomuser@example.com", passwordEncoder.encode("password123"));
        testUser.setName("Room User");
        testUser = userRepository.save(testUser);

        testRoom = new Room(testUser, "Test Room");
        testRoom.setDescription("Test Room Description");
        testRoom.setCategory("Technology");
        testRoom.setMaxCollaborators(10);
        testRoom.setMeetingTime(Instant.now().plusSeconds(3600));
        testRoom.setMeetingType("online");
        testRoom.setLocation(null);
        testRoom.getCollaborators().add(testUser);
        testRoom.getAdmins().add(testUser);
        testRoom = roomRepository.save(testRoom);
    }

    @Test
    void shouldCreateRoom() {
        Map<String, Object> request = new HashMap<>();
        request.put("description", "New Room Description");
        request.put("category", "Science");
        request.put("maxCollaborators", 5);
        request.put("meetingTime", Instant.now().plusSeconds(7200).toString());
        request.put("meetingType", "offline");
        request.put("location", "Moscow, Russia");

        given()
            .contentType(ContentType.JSON)
            .queryParam("userId", testUser.getId())
            .body(request)
            .when()
            .post("/rooms")
            .then()
            .statusCode(201)
            .body("description", equalTo("New Room Description"))
            .body("category", equalTo("Science"))
            .body("maxCollaborators", equalTo(5))
            .body("meetingType", equalTo("offline"));
    }

    @Test
    void shouldReturn400WhenMaxCollaboratorsIsInvalid() {
        Map<String, Object> request = new HashMap<>();
        request.put("description", "Invalid Room");
        request.put("maxCollaborators", -1);

        given()
            .contentType(ContentType.JSON)
            .queryParam("userId", testUser.getId())
            .body(request)
            .when()
            .post("/rooms")
            .then()
            .statusCode(400);
    }

    @Test
    void shouldJoinRoom() {
        User newUser = new User("newuser", "newuser@example.com", passwordEncoder.encode("password123"));
        newUser = userRepository.save(newUser);

        Map<String, Long> request = new HashMap<>();
        request.put("userId", newUser.getId());

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/rooms/{roomId}/join", testRoom.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldReturn409WhenJoiningFullRoom() {
        Room smallRoom = new Room(testUser, "Small Room");
        smallRoom.setMaxCollaborators(1);
        smallRoom.getCollaborators().add(testUser);
        smallRoom = roomRepository.save(smallRoom);

        User anotherUser = new User("anotheruser", "another@example.com", passwordEncoder.encode("password123"));
        anotherUser = userRepository.save(anotherUser);

        Map<String, Long> request = new HashMap<>();
        request.put("userId", anotherUser.getId());

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/rooms/{roomId}/join", smallRoom.getId())
            .then()
            .statusCode(409);
    }

    @Test
    void shouldGetRoomBrief() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/rooms/{roomId}/brief", testRoom.getId())
            .then()
            .statusCode(200)
            .body("roomId", equalTo(testRoom.getId().intValue()))
            .body("briefDescription", equalTo("Test Room Description"));
    }

    @Test
    void shouldReturn404WhenRoomNotFound() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/rooms/{roomId}/brief", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    void shouldOpenRoomChat() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("userId", testUser.getId())
            .when()
            .get("/rooms/{roomId}/chat", testRoom.getId())
            .then()
            .statusCode(200)
            .body("roomId", equalTo(testRoom.getId().intValue()))
            .body("messages", isA(List.class));
    }

    @Test
    void shouldReturn403WhenUserNotMemberOfRoom() {
        User nonMember = new User("nonmember", "nonmember@example.com", passwordEncoder.encode("password123"));
        nonMember = userRepository.save(nonMember);

        given()
            .contentType(ContentType.JSON)
            .queryParam("userId", nonMember.getId())
            .when()
            .get("/rooms/{roomId}/chat", testRoom.getId())
            .then()
            .statusCode(403);
    }

    @Test
    void shouldGetRoomNotificationSettings() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/rooms/{roomId}/settings/notifications", testRoom.getId())
            .then()
            .statusCode(200)
            .body("invitationNotifications", notNullValue())
            .body("messageNotifications", notNullValue())
            .body("removalNotifications", notNullValue());
    }

    @Test
    void shouldUpdateRoomNotificationSettings() {
        Map<String, Boolean> request = new HashMap<>();
        request.put("invitationNotifications", false);
        request.put("messageNotifications", true);
        request.put("removalNotifications", false);

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/rooms/{roomId}/settings/notifications", testRoom.getId())
            .then()
            .statusCode(200)
            .body("invitationNotifications", equalTo(false))
            .body("messageNotifications", equalTo(true))
            .body("removalNotifications", equalTo(false));
    }

    @Test
    void shouldAddMessageToRoom() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("creatorId", testUser.getId())
            .formParam("text", "Hello, this is a test message!")
            .when()
            .post("/rooms/{roomId}/messages/add", testRoom.getId())
            .then()
            .statusCode(200)
            .body("text", equalTo("Hello, this is a test message!"));
    }

    @Test
    void shouldAddUserToRoom() {
        User newUser = new User("newuser", "newuser@example.com", passwordEncoder.encode("password123"));
        newUser = userRepository.save(newUser);

        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", newUser.getId())
            .when()
            .post("/rooms/{roomId}/participants/add", testRoom.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldRemoveUserFromRoom() {
        User userToRemove = new User("toremove", "toremove@example.com", passwordEncoder.encode("password123"));
        userToRemove = userRepository.save(userToRemove);

        testRoom.getCollaborators().add(userToRemove);
        roomRepository.save(testRoom);

        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", userToRemove.getId())
            .when()
            .delete("/rooms/{roomId}/participants/remove", testRoom.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldCreateRoomWithLegacyEndpoint() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", testUser.getId())
            .formParam("name", "Legacy Room")
            .when()
            .post("/rooms/create")
            .then()
            .statusCode(201)
            .body("name", equalTo("Legacy Room"));
    }
}

