package com.mipt.CoActivity.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class MembershipRequestResponse {
    private Long id;
    private Long roomId;
    private String roomName;
    private Long requesterId;
    private String requesterName;
    private String status; // "pending", "approved", "rejected", "cancelled"
    private String message;
    private Instant createdAt;
    private Instant respondedAt;
    private Long responderId;
    private String responderName;
}

