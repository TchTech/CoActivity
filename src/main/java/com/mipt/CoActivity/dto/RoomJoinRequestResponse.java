package com.mipt.CoActivity.dto;

import com.mipt.CoActivity.model.RoomJoinRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for room join request operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomJoinRequestResponse {
    private Long id;
    private Long roomId;
    private String roomName;
    private Long userId;
    private String userName;
    private String status; // "pending", "approved", "rejected", "cancelled"
    private String message;
    private Instant createdAt;
    private Instant respondedAt;
    private Long responderId;
    private String responderName;
    private String rejectionReason;
    private Instant lastRejectedAt;

    /**
     * Create a RoomJoinRequestResponse from a RoomJoinRequest entity.
     * 
     * @param entity The RoomJoinRequest entity
     * @return RoomJoinRequestResponse DTO
     */
    public static RoomJoinRequestResponse fromEntity(RoomJoinRequest entity) {
        RoomJoinRequestResponse response = new RoomJoinRequestResponse();
        response.setId(entity.getId());
        if (entity.getRoom() != null) {
            response.setRoomId(entity.getRoom().getId());
            response.setRoomName(entity.getRoom().getName());
        }
        if (entity.getUser() != null) {
            response.setUserId(entity.getUser().getId());
            response.setUserName(entity.getUser().getName() != null 
                ? entity.getUser().getName() 
                : entity.getUser().getUsername());
        }
        response.setStatus(entity.getStatus());
        response.setMessage(entity.getMessage());
        response.setCreatedAt(entity.getCreatedAt());
        response.setRespondedAt(entity.getRespondedAt());
        if (entity.getResponder() != null) {
            response.setResponderId(entity.getResponder().getId());
            response.setResponderName(entity.getResponder().getName() != null 
                ? entity.getResponder().getName() 
                : entity.getResponder().getUsername());
        }
        response.setRejectionReason(entity.getRejectionReason());
        response.setLastRejectedAt(entity.getLastRejectedAt());
        return response;
    }
}

