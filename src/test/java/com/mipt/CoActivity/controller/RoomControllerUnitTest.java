package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.dto.*;
import com.mipt.CoActivity.exception.BadRequestException;
import com.mipt.CoActivity.exception.ForbiddenException;
import com.mipt.CoActivity.exception.ResourceNotFoundException;
import com.mipt.CoActivity.model.Message;
import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testRoomId;
    private Long testUserId;
    private Long testMessageId;
    private Long testAdminId;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        testRoomId = 1L;
        testUserId = 1L;
        testMessageId = 1L;
        testAdminId = 2L;

        User testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(testUserId);
        Room testRoom = new Room(testUser, "Test Room");
        testRoom.setId(testRoomId);

        testMessage = new Message(testRoom, testUser, "Test message");
        testMessage.setId(testMessageId);
        testMessage.setDate(Instant.now());
    }

    @Test
    void shouldApproveJoinRequestSuccessfully() throws Exception {
        ApproveJoinRequestRequest request = new ApproveJoinRequestRequest();
        request.setAdminId(testAdminId);

        doNothing().when(roomService).approveJoinRequest(eq(testRoomId), eq(testUserId), any(ApproveJoinRequestRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/join-requests/{targetUserId}/approve", testRoomId, testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(roomService, times(1)).approveJoinRequest(eq(testRoomId), eq(testUserId), any(ApproveJoinRequestRequest.class));
    }

    @Test
    void shouldReturn403WhenNonAdminTriesToApproveJoinRequest() throws Exception {
        ApproveJoinRequestRequest request = new ApproveJoinRequestRequest();
        request.setAdminId(testAdminId);

        doThrow(new ForbiddenException("Only administrators can approve join requests"))
                .when(roomService).approveJoinRequest(eq(testRoomId), eq(testUserId), any(ApproveJoinRequestRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/join-requests/{targetUserId}/approve", testRoomId, testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only administrators can approve join requests"));
    }

    @Test
    void shouldReturn404WhenRoomNotFoundForApproveJoinRequest() throws Exception {
        ApproveJoinRequestRequest request = new ApproveJoinRequestRequest();
        request.setAdminId(testAdminId);

        doThrow(new ResourceNotFoundException("Room not found"))
                .when(roomService).approveJoinRequest(eq(testRoomId), eq(testUserId), any(ApproveJoinRequestRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/join-requests/{targetUserId}/approve", testRoomId, testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectJoinRequestSuccessfully() throws Exception {
        RejectJoinRequestRequest request = new RejectJoinRequestRequest();
        request.setAdminId(testAdminId);

        doNothing().when(roomService).rejectJoinRequest(eq(testRoomId), eq(testUserId), any(RejectJoinRequestRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/join-requests/{targetUserId}/reject", testRoomId, testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(roomService, times(1)).rejectJoinRequest(eq(testRoomId), eq(testUserId), any(RejectJoinRequestRequest.class));
    }

    @Test
    void shouldReturn403WhenNonAdminTriesToRejectJoinRequest() throws Exception {
        RejectJoinRequestRequest request = new RejectJoinRequestRequest();
        request.setAdminId(testAdminId);

        doThrow(new ForbiddenException("Only administrators can reject join requests"))
                .when(roomService).rejectJoinRequest(eq(testRoomId), eq(testUserId), any(RejectJoinRequestRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/join-requests/{targetUserId}/reject", testRoomId, testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldSendRoomMessageSuccessfully() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(testUserId);
        request.setContent("Hello, this is a test message!");

        when(roomService.sendRoomMessage(eq(testRoomId), any(SendMessageRequest.class)))
                .thenReturn(testMessage);

        mockMvc.perform(post("/rooms/{roomId}/chat/messages", testRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testMessageId.intValue()))
                .andExpect(jsonPath("$.text").value("Test message"));
    }

    @Test
    void shouldReturn403WhenNonMemberTriesToSendMessage() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(testUserId);
        request.setContent("Hello!");

        when(roomService.sendRoomMessage(eq(testRoomId), any(SendMessageRequest.class)))
                .thenThrow(new ForbiddenException("User is not a member of this room"));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages", testRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User is not a member of this room"));
    }

    @Test
    void shouldReturn404WhenRoomNotFoundForSendMessage() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(testUserId);
        request.setContent("Hello!");

        when(roomService.sendRoomMessage(eq(testRoomId), any(SendMessageRequest.class)))
                .thenThrow(new ResourceNotFoundException("Room not found"));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages", testRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenMessageContentIsEmpty() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(testUserId);
        request.setContent("");

        when(roomService.sendRoomMessage(eq(testRoomId), any(SendMessageRequest.class)))
                .thenThrow(new BadRequestException("Content cannot be empty"));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages", testRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReportMessageSuccessfully() throws Exception {
        ReportMessageRequest request = new ReportMessageRequest();
        request.setReporterId(testUserId);
        request.setViolationType("platformRules");
        request.setComment("This message contains inappropriate content");

        doNothing().when(roomService).reportMessage(eq(testRoomId), eq(testMessageId), any(ReportMessageRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/report", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(roomService, times(1)).reportMessage(eq(testRoomId), eq(testMessageId), any(ReportMessageRequest.class));
    }

    @Test
    void shouldReturn400WhenReportViolationTypeIsInvalid() throws Exception {
        ReportMessageRequest request = new ReportMessageRequest();
        request.setReporterId(testUserId);
        request.setViolationType("invalidType");
        request.setComment("Test comment");

        doThrow(new BadRequestException("Invalid violation type"))
                .when(roomService).reportMessage(eq(testRoomId), eq(testMessageId), any(ReportMessageRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/report", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenMessageNotFoundForReport() throws Exception {
        ReportMessageRequest request = new ReportMessageRequest();
        request.setReporterId(testUserId);
        request.setViolationType("platformRules");

        doThrow(new ResourceNotFoundException("Message not found"))
                .when(roomService).reportMessage(eq(testRoomId), eq(testMessageId), any(ReportMessageRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/report", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldConfirmMessageViolationSuccessfully() throws Exception {
        ConfirmViolationRequest request = new ConfirmViolationRequest();
        request.setAdminId(testAdminId);
        request.setSanction("warning");

        doNothing().when(roomService).confirmMessageViolation(eq(testRoomId), eq(testMessageId), any(ConfirmViolationRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/moderation/confirm", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(roomService, times(1)).confirmMessageViolation(eq(testRoomId), eq(testMessageId), any(ConfirmViolationRequest.class));
    }

    @Test
    void shouldReturn403WhenNonAdminTriesToConfirmViolation() throws Exception {
        ConfirmViolationRequest request = new ConfirmViolationRequest();
        request.setAdminId(testAdminId);
        request.setSanction("warning");

        doThrow(new ForbiddenException("Only administrators can confirm violations"))
                .when(roomService).confirmMessageViolation(eq(testRoomId), eq(testMessageId), any(ConfirmViolationRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/moderation/confirm", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400WhenSanctionTypeIsInvalid() throws Exception {
        ConfirmViolationRequest request = new ConfirmViolationRequest();
        request.setAdminId(testAdminId);
        request.setSanction("invalidSanction");

        doThrow(new BadRequestException("Invalid sanction type"))
                .when(roomService).confirmMessageViolation(eq(testRoomId), eq(testMessageId), any(ConfirmViolationRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/moderation/confirm", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMessageViolationSuccessfully() throws Exception {
        RejectViolationRequest request = new RejectViolationRequest();
        request.setAdminId(testAdminId);

        doNothing().when(roomService).rejectMessageViolation(eq(testRoomId), eq(testMessageId), any(RejectViolationRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/moderation/reject", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(roomService, times(1)).rejectMessageViolation(eq(testRoomId), eq(testMessageId), any(RejectViolationRequest.class));
    }

    @Test
    void shouldReturn403WhenNonAdminTriesToRejectViolation() throws Exception {
        RejectViolationRequest request = new RejectViolationRequest();
        request.setAdminId(testAdminId);

        doThrow(new ForbiddenException("Only administrators can reject violation reports"))
                .when(roomService).rejectMessageViolation(eq(testRoomId), eq(testMessageId), any(RejectViolationRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/moderation/reject", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenMessageNotFoundForRejectViolation() throws Exception {
        RejectViolationRequest request = new RejectViolationRequest();
        request.setAdminId(testAdminId);

        doThrow(new ResourceNotFoundException("Message not found"))
                .when(roomService).rejectMessageViolation(eq(testRoomId), eq(testMessageId), any(RejectViolationRequest.class));

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/moderation/reject", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeserializeSendMessageRequestCorrectly() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(testUserId);
        request.setContent("Test message content");

        when(roomService.sendRoomMessage(eq(testRoomId), any(SendMessageRequest.class)))
                .thenReturn(testMessage);

        String jsonRequest = """
                {
                    "senderId": 1,
                    "content": "Test message content"
                }
                """;

        mockMvc.perform(post("/rooms/{roomId}/chat/messages", testRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDeserializeReportMessageRequestCorrectly() throws Exception {
        ReportMessageRequest request = new ReportMessageRequest();
        request.setReporterId(testUserId);
        request.setViolationType("roomRules");
        request.setComment("Inappropriate content");

        doNothing().when(roomService).reportMessage(eq(testRoomId), eq(testMessageId), any(ReportMessageRequest.class));

        String jsonRequest = """
                {
                    "reporterId": 1,
                    "violationType": "roomRules",
                    "comment": "Inappropriate content"
                }
                """;

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/report", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeserializeApproveJoinRequestRequestCorrectly() throws Exception {
        ApproveJoinRequestRequest request = new ApproveJoinRequestRequest();
        request.setAdminId(testAdminId);

        doNothing().when(roomService).approveJoinRequest(eq(testRoomId), eq(testUserId), any(ApproveJoinRequestRequest.class));

        String jsonRequest = """
                {
                    "adminId": 2
                }
                """;

        mockMvc.perform(post("/rooms/{roomId}/join-requests/{targetUserId}/approve", testRoomId, testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeserializeConfirmViolationRequestCorrectly() throws Exception {
        ConfirmViolationRequest request = new ConfirmViolationRequest();
        request.setAdminId(testAdminId);
        request.setSanction("ban");

        doNothing().when(roomService).confirmMessageViolation(eq(testRoomId), eq(testMessageId), any(ConfirmViolationRequest.class));

        String jsonRequest = """
                {
                    "adminId": 2,
                    "sanction": "ban"
                }
                """;

        mockMvc.perform(post("/rooms/{roomId}/chat/messages/{messageId}/moderation/confirm", testRoomId, testMessageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk());
    }

    @Test
    void shouldSerializeMessageResponseCorrectly() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(testUserId);
        request.setContent("Test message");

        when(roomService.sendRoomMessage(eq(testRoomId), any(SendMessageRequest.class)))
                .thenReturn(testMessage);

        mockMvc.perform(post("/rooms/{roomId}/chat/messages", testRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").exists());
    }
}


