package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.BaseIntegrationTest;
import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.RoomJoinRequest;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Room Join Request API endpoints.
 * Tests the full HTTP flow from request creation to approval.
 * 
 * DISABLED: Integration tests with Testcontainers are slow.
 * Use unit tests (RoomJoinRequestServiceTest) instead for faster feedback.
 */
@Disabled("Integration tests are slow - use unit tests instead")
@Transactional
class RoomJoinRequestControllerTest extends BaseIntegrationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomJoinRequestRepository roomJoinRequestRepository;

    @Autowired
    private RoomJoinRequestHistoryRepository roomJoinRequestHistoryRepository;

    private User roomCreator;
    private User admin;
    private User applicant;
    private Room testRoom;
    private Long roomId;
    private Long creatorId;
    private Long adminId;
    private Long applicantId;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        // Create users
        roomCreator = new User("creator", "creator@test.com", "hashedPassword");
        roomCreator.setName("Room Creator");
        roomCreator = userRepository.save(roomCreator);
        creatorId = roomCreator.getId();

        admin = new User("admin", "admin@test.com", "hashedPassword");
        admin.setName("Admin User");
        admin = userRepository.save(admin);
        adminId = admin.getId();

        applicant = new User("applicant", "applicant@test.com", "hashedPassword");
        applicant.setName("Applicant User");
        applicant = userRepository.save(applicant);
        applicantId = applicant.getId();

        // Create room
        testRoom = new Room(roomCreator, "Test Room");
        testRoom.setJoinType("by_application");
        testRoom.setMaxCollaborators(5);
        testRoom.setIsClosed(false);
        testRoom.getCollaborators().add(roomCreator);
        testRoom.getAdmins().add(roomCreator);
        testRoom.getAdmins().add(admin);
        testRoom = roomRepository.save(testRoom);
        roomId = testRoom.getId();
    }

    @Test
    void testCreateRequest_Success() {
        // Arrange
        CreateRoomJoinRequestRequest request = new CreateRoomJoinRequestRequest();
        request.setMessage("I want to join this room");

        // Act & Assert
        RoomJoinRequestResponse response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .queryParam("userId", applicantId)
                .when()
                .post("/api/rooms/{roomId}/requests", roomId)
                .then()
                .statusCode(201)
                .extract()
                .as(RoomJoinRequestResponse.class);

        // Verify response
        assertNotNull(response.getId());
        assertEquals(roomId, response.getRoomId());
        assertEquals(applicantId, response.getUserId());
        assertEquals("pending", response.getStatus());
        assertEquals("I want to join this room", response.getMessage());

        // Verify database
        RoomJoinRequest savedRequest = roomJoinRequestRepository.findById(response.getId()).orElseThrow();
        assertEquals("pending", savedRequest.getStatus());
    }

    @Test
    void testCreateRequest_RejectsOpenRoom() {
        // Arrange
        testRoom.setJoinType("open");
        roomRepository.save(testRoom);

        CreateRoomJoinRequestRequest request = new CreateRoomJoinRequestRequest();
        request.setMessage("Trying to request open room");

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .queryParam("userId", applicantId)
                .when()
                .post("/api/rooms/{roomId}/requests", roomId)
                .then()
                .statusCode(400)
                .body(containsString("by_application"));
    }

    @Test
    void testGetPendingRequests_AdminView() {
        // Arrange - Create a pending request first
        RoomJoinRequest pendingRequest = new RoomJoinRequest(testRoom, applicant);
        pendingRequest.setStatus("pending");
        roomJoinRequestRepository.save(pendingRequest);

        // Act & Assert
        List<RoomJoinRequestResponse> requests = given()
                .queryParam("userId", creatorId)
                .when()
                .get("/api/rooms/{roomId}/requests", roomId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("", RoomJoinRequestResponse.class);

        assertFalse(requests.isEmpty());
        assertEquals(applicantId, requests.get(0).getUserId());
        assertEquals("pending", requests.get(0).getStatus());
    }

    @Test
    void testGetPendingRequests_ForbiddenForNonAdmin() {
        // Act & Assert
        given()
                .queryParam("userId", applicantId) // Not an admin
                .when()
                .get("/api/rooms/{roomId}/requests", roomId)
                .then()
                .statusCode(403);
    }

    @Test
    void testApproveRequest_Success() {
        // Arrange - Create a pending request
        RoomJoinRequest pendingRequest = new RoomJoinRequest(testRoom, applicant);
        pendingRequest.setStatus("pending");
        pendingRequest = roomJoinRequestRepository.save(pendingRequest);

        ApproveJoinRequestRequest approveRequest = new ApproveJoinRequestRequest();
        approveRequest.setAdminId(adminId);

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body(approveRequest)
                .queryParam("targetUserId", applicantId)
                .when()
                .post("/api/rooms/{roomId}/requests/{requestId}/approve", roomId, pendingRequest.getId())
                .then()
                .statusCode(200);

        // Verify request is approved
        RoomJoinRequest approvedRequest = roomJoinRequestRepository.findById(pendingRequest.getId()).orElseThrow();
        assertEquals("approved", approvedRequest.getStatus());
        assertNotNull(approvedRequest.getRespondedAt());

        // Verify user is in collaborators
        Room updatedRoom = roomRepository.findById(roomId).orElseThrow();
        assertTrue(updatedRoom.getCollaborators().stream()
                .anyMatch(u -> u.getId().equals(applicantId)));
    }

    @Test
    void testRejectRequest_Success() {
        // Arrange - Create a pending request
        RoomJoinRequest pendingRequest = new RoomJoinRequest(testRoom, applicant);
        pendingRequest.setStatus("pending");
        pendingRequest = roomJoinRequestRepository.save(pendingRequest);

        RejectJoinRequestRequest rejectRequest = new RejectJoinRequestRequest();
        rejectRequest.setAdminId(adminId);
        rejectRequest.setReason("Not suitable");

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body(rejectRequest)
                .queryParam("targetUserId", applicantId)
                .when()
                .post("/api/rooms/{roomId}/requests/{requestId}/reject", roomId, pendingRequest.getId())
                .then()
                .statusCode(200);

        // Verify request is rejected
        RoomJoinRequest rejectedRequest = roomJoinRequestRepository.findById(pendingRequest.getId()).orElseThrow();
        assertEquals("rejected", rejectedRequest.getStatus());
        assertNotNull(rejectedRequest.getLastRejectedAt());
        assertEquals("Not suitable", rejectedRequest.getRejectionReason());
    }

    @Test
    void testCancelRequest_Success() {
        // Arrange - Create a pending request
        RoomJoinRequest pendingRequest = new RoomJoinRequest(testRoom, applicant);
        pendingRequest.setStatus("pending");
        pendingRequest = roomJoinRequestRepository.save(pendingRequest);

        // Act & Assert
        given()
                .queryParam("userId", applicantId)
                .when()
                .delete("/api/rooms/{roomId}/requests/{requestId}", roomId, pendingRequest.getId())
                .then()
                .statusCode(200);

        // Verify request is cancelled
        RoomJoinRequest cancelledRequest = roomJoinRequestRepository.findById(pendingRequest.getId()).orElseThrow();
        assertEquals("cancelled", cancelledRequest.getStatus());
    }

    @Test
    void testCancelRequest_ForbiddenForNonOwner() {
        // Arrange - Create a pending request
        RoomJoinRequest pendingRequest = new RoomJoinRequest(testRoom, applicant);
        pendingRequest.setStatus("pending");
        pendingRequest = roomJoinRequestRepository.save(pendingRequest);

        // Act & Assert
        given()
                .queryParam("userId", adminId) // Not the request owner
                .when()
                .delete("/api/rooms/{roomId}/requests/{requestId}", roomId, pendingRequest.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void testGetMyPendingRequests() {
        // Arrange - Create pending requests for applicant
        RoomJoinRequest pendingRequest = new RoomJoinRequest(testRoom, applicant);
        pendingRequest.setStatus("pending");
        roomJoinRequestRepository.save(pendingRequest);

        // Act & Assert
        List<RoomJoinRequestResponse> requests = given()
                .queryParam("userId", applicantId)
                .when()
                .get("/api/rooms/my-applications")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("", RoomJoinRequestResponse.class);

        assertFalse(requests.isEmpty());
        assertEquals(applicantId, requests.get(0).getUserId());
        assertEquals("pending", requests.get(0).getStatus());
    }

    @Test
    void testCloseRoom_AutoRejectsPendingRequests() {
        // Arrange - Create pending requests
        RoomJoinRequest pendingRequest1 = new RoomJoinRequest(testRoom, applicant);
        pendingRequest1.setStatus("pending");
        roomJoinRequestRepository.save(pendingRequest1);

        User applicant2 = new User("applicant2", "applicant2@test.com", "hashedPassword");
        applicant2 = userRepository.save(applicant2);
        RoomJoinRequest pendingRequest2 = new RoomJoinRequest(testRoom, applicant2);
        pendingRequest2.setStatus("pending");
        roomJoinRequestRepository.save(pendingRequest2);

        CloseRoomRequest closeRequest = new CloseRoomRequest();
        closeRequest.setUserId(creatorId);

        // Act
        given()
                .contentType(ContentType.JSON)
                .body(closeRequest)
                .when()
                .post("/api/rooms/{roomId}/close", roomId)
                .then()
                .statusCode(200);

        // Verify room is closed
        Room closedRoom = roomRepository.findById(roomId).orElseThrow();
        assertTrue(closedRoom.getIsClosed());
        assertNotNull(closedRoom.getClosedAt());

        // Verify all pending requests are rejected
        List<RoomJoinRequest> rejectedRequests = roomJoinRequestRepository.findPendingRequestsByRoom(roomId);
        assertTrue(rejectedRequests.isEmpty(), "All pending requests should be rejected");

        List<RoomJoinRequest> allRequests = roomJoinRequestRepository.findByRoomId(roomId);
        allRequests.forEach(req -> {
            assertEquals("rejected", req.getStatus());
            assertNotNull(req.getRejectionReason());
        });
    }

    @Test
    void testFullFlow_CreateApproveVerify() {
        // Step 1: Create request
        CreateRoomJoinRequestRequest createRequest = new CreateRoomJoinRequestRequest();
        createRequest.setMessage("Please accept me");

        RoomJoinRequestResponse createdRequest = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .queryParam("userId", applicantId)
                .when()
                .post("/api/rooms/{roomId}/requests", roomId)
                .then()
                .statusCode(201)
                .extract()
                .as(RoomJoinRequestResponse.class);

        assertNotNull(createdRequest.getId());
        assertEquals("pending", createdRequest.getStatus());

        // Step 2: Admin views pending requests
        List<RoomJoinRequestResponse> pendingRequests = given()
                .queryParam("userId", adminId)
                .when()
                .get("/api/rooms/{roomId}/requests", roomId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("", RoomJoinRequestResponse.class);

        assertEquals(1, pendingRequests.size());
        assertEquals(createdRequest.getId(), pendingRequests.get(0).getId());

        // Step 3: Approve request
        ApproveJoinRequestRequest approveRequest = new ApproveJoinRequestRequest();
        approveRequest.setAdminId(adminId);

        given()
                .contentType(ContentType.JSON)
                .body(approveRequest)
                .queryParam("targetUserId", applicantId)
                .when()
                .post("/api/rooms/{roomId}/requests/{requestId}/approve", roomId, createdRequest.getId())
                .then()
                .statusCode(200);

        // Step 4: Verify user is in collaborators
        Room updatedRoom = roomRepository.findById(roomId).orElseThrow();
        assertTrue(updatedRoom.getCollaborators().stream()
                .anyMatch(u -> u.getId().equals(applicantId)));

        // Step 5: Verify request is approved
        RoomJoinRequest approvedRequest = roomJoinRequestRepository.findById(createdRequest.getId()).orElseThrow();
        assertEquals("approved", approvedRequest.getStatus());
        assertNotNull(approvedRequest.getRespondedAt());
    }
}

