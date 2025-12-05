package com.mipt.CoActivity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized notification data schema.
 * This DTO represents the JSON structure stored in the Notification.data field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationData {
    private Long roomId;
    private Long requestId;
    private Long requesterId;
    private Long responderId;
    private String reason;
    private Long requestedUserId;
    private String requestedUserName;
    private Long creatorId;
    private String creatorName;
    
    /**
     * Convert to JSON string for storage in Notification.data field.
     */
    public String toJson() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize notification data to JSON", e);
        }
    }
    
    /**
     * Parse JSON string from Notification.data field.
     */
    public static NotificationData fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, NotificationData.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse notification data from JSON: " + json, e);
        }
    }
}

