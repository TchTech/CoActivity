package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.model.*;
import com.mipt.CoActivity.service.RoomJoinRequestService;
import com.mipt.CoActivity.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = RoomController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class RoomControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @MockBean
    private RoomJoinRequestService roomJoinRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    private Room testRoom;
    private User testUser;
    private RoomDetailsResponse roomDetailsResponse;
    private RoomBriefResponse roomBriefResponse;
    private RoomChatResponse roomChatResponse;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);

        testRoom = new Room(testUser, "Test Room");
        testRoom.setId(1L);
        testRoom.setDescription("Test Description");
        testRoom.setCategory("Technology");

        roomDetailsResponse = new RoomDetailsResponse();
        roomDetailsResponse.setId(1L);
        roomDetailsResponse.setName("Test Room");
        roomDetailsResponse.setDescription("Test Description");

        roomBriefResponse = new RoomBriefResponse();
        roomBriefResponse.setRoomId(1L);
        roomBriefResponse.setBriefDescription("Test Description");

        roomChatResponse = new RoomChatResponse();
        roomChatResponse.setRoomId(1L);
        roomChatResponse.setMessages(new ArrayList<>());
    }

    @Test
    void testCreateRoom_Success() throws Exception {
        // Given
        CreateRoomRequest request = new CreateRoomRequest();
        request.setDescription("New Room");
        request.setCategory("Science");
        when(roomService.createRoom(eq(1L), any(CreateRoomRequest.class))).thenReturn(testRoom);

        // When & Then
        mockMvc.perform(post("/api/rooms")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testJoinRoom_Success() throws Exception {
        // Given
        JoinRoomRequest request = new JoinRoomRequest();
        request.setUserId(2L);
        doNothing().when(roomService).joinRoom(eq(1L), any(JoinRoomRequest.class));

        // When & Then
        mockMvc.perform(post("/api/rooms/1/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetRoomDetails_Success() throws Exception {
        // Given
        when(roomService.getRoomDetails(1L)).thenReturn(roomDetailsResponse);

        // When & Then
        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testGetRoomBrief_Success() throws Exception {
        // Given
        when(roomService.getRoomBrief(1L)).thenReturn(roomBriefResponse);

        // When & Then
        mockMvc.perform(get("/api/rooms/1/brief"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1));
    }

    @Test
    void testOpenRoomChat_Success() throws Exception {
        // Given
        when(roomService.openRoomChat(1L, 1L)).thenReturn(roomChatResponse);

        // When & Then
        mockMvc.perform(get("/api/rooms/1/chat")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1));
    }

    @Test
    void testGetRoomNotificationSettings_Success() throws Exception {
        // Given
        RoomNotificationSettings settings = new RoomNotificationSettings();
        settings.setInvitationNotifications(true);
        when(roomService.getRoomNotificationSettings(1L)).thenReturn(settings);

        // When & Then
        mockMvc.perform(get("/api/rooms/1/settings/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationNotifications").value(true));
    }

    @Test
    void testUpdateRoomNotificationSettings_Success() throws Exception {
        // Given
        RoomNotificationSettingsRequest request = new RoomNotificationSettingsRequest();
        request.setInvitationNotifications(false);
        RoomNotificationSettings updatedSettings = new RoomNotificationSettings();
        updatedSettings.setInvitationNotifications(false);
        when(roomService.updateRoomNotificationSettings(eq(1L), any(RoomNotificationSettingsRequest.class)))
                .thenReturn(updatedSettings);

        // When & Then
        mockMvc.perform(put("/api/rooms/1/settings/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationNotifications").value(false));
    }

    @Test
    void testCreateRequest_Success() throws Exception {
        // Given
        CreateRoomJoinRequestRequest request = new CreateRoomJoinRequestRequest();
        request.setMessage("I want to join");
        RoomJoinRequest joinRequest = new RoomJoinRequest();
        joinRequest.setId(1L);
        joinRequest.setStatus("pending");
        RoomJoinRequestResponse response = RoomJoinRequestResponse.fromEntity(joinRequest);
        when(roomJoinRequestService.createRequest(eq(1L), eq(2L), any())).thenReturn(joinRequest);

        // When & Then
        mockMvc.perform(post("/api/rooms/1/requests")
                .param("userId", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testGetPendingRequests_Success() throws Exception {
        // Given
        RoomJoinRequest joinRequest = new RoomJoinRequest();
        joinRequest.setId(1L);
        joinRequest.setStatus("pending");
        List<RoomJoinRequest> requests = List.of(joinRequest);
        when(roomJoinRequestService.getPendingRequests(1L, 1L)).thenReturn(requests);

        // When & Then
        mockMvc.perform(get("/api/rooms/1/requests")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testApproveRequest_Success() throws Exception {
        // Given
        ApproveJoinRequestRequest request = new ApproveJoinRequestRequest();
        request.setAdminId(1L);
        doNothing().when(roomJoinRequestService).approveRequest(eq(1L), eq(1L), eq(2L), any(ApproveJoinRequestRequest.class));

        // When & Then
        mockMvc.perform(post("/api/rooms/1/requests/1/approve")
                .param("targetUserId", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testRejectRequest_Success() throws Exception {
        // Given
        RejectJoinRequestRequest request = new RejectJoinRequestRequest();
        request.setReason("Not suitable");
        request.setAdminId(1L);
        doNothing().when(roomJoinRequestService).rejectRequest(eq(1L), eq(1L), eq(2L), any(RejectJoinRequestRequest.class));

        // When & Then
        mockMvc.perform(post("/api/rooms/1/requests/1/reject")
                .param("targetUserId", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCancelRequest_Success() throws Exception {
        // Given
        doNothing().when(roomJoinRequestService).cancelRequest(eq(1L), eq(1L), eq(2L));

        // When & Then
        mockMvc.perform(delete("/api/rooms/1/requests/1")
                .param("userId", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void testCloseRoom_Success() throws Exception {
        // Given
        CloseRoomRequest request = new CloseRoomRequest();
        request.setUserId(1L);
        doNothing().when(roomService).closeRoom(eq(1L), eq(1L));

        // When & Then
        mockMvc.perform(post("/api/rooms/1/close")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetMyPendingRequests_Success() throws Exception {
        // Given
        RoomJoinRequest joinRequest = new RoomJoinRequest();
        joinRequest.setId(1L);
        joinRequest.setStatus("pending");
        List<RoomJoinRequest> requests = List.of(joinRequest);
        when(roomJoinRequestService.getMyPendingRequests(1L)).thenReturn(requests);

        // When & Then
        mockMvc.perform(get("/api/rooms/my-applications")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testSendRoomMessage_Success() throws Exception {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(1L);
        request.setContent("Hello");
        Message message = new Message();
        message.setId(1L);
        message.setText("Hello");
        when(roomService.sendRoomMessage(eq(1L), any(SendMessageRequest.class))).thenReturn(message);

        // When & Then
        mockMvc.perform(post("/api/rooms/1/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testSearchRooms_Success() throws Exception {
        // Given
        List<Room> rooms = new ArrayList<>();
        when(roomService.searchRoomsWithFilters(any(), any(), any(), any(), any())).thenReturn(rooms);

        // When & Then
        mockMvc.perform(get("/api/rooms/search")
                .param("query", "test")
                .param("category", "Technology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testRequestRating_Success() throws Exception {
        // Given
        doNothing().when(roomService).requestRating(eq(1L), eq(2L), eq(1L));

        // When & Then
        mockMvc.perform(post("/api/rooms/1/rating-requests")
                .param("requestedUserId", "2")
                .param("requesterUserId", "1"))
                .andExpect(status().isOk());
    }
}

