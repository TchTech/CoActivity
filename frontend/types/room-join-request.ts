/**
 * Type definitions for Room Join Request system
 */

/**
 * Room Join Request status types
 */
export type RoomJoinRequestStatus = 
  | "pending" 
  | "approved" 
  | "rejected" 
  | "cancelled";

/**
 * Room Join Request response from API
 * Matches RoomJoinRequestResponse DTO from backend
 */
export interface RoomJoinRequest {
  id: number;
  roomId: number;
  roomName?: string;
  userId: number;
  userName?: string;
  status: RoomJoinRequestStatus;
  message?: string;
  createdAt: string; // ISO 8601 timestamp
  respondedAt?: string; // ISO 8601 timestamp
  responderId?: number;
  responderName?: string;
  rejectionReason?: string;
  lastRejectedAt?: string; // ISO 8601 timestamp (for cooldown tracking)
}

/**
 * Request DTO for creating a room join request
 * Matches CreateRoomJoinRequestRequest DTO from backend
 */
export interface CreateRoomJoinRequestRequest {
  message?: string; // Max 500 characters
}

/**
 * Request DTO for approving a join request
 * Matches ApproveJoinRequestRequest DTO from backend
 */
export interface ApproveJoinRequestRequest {
  adminId: number;
}

/**
 * Request DTO for rejecting a join request
 * Matches RejectJoinRequestRequest DTO from backend
 */
export interface RejectJoinRequestRequest {
  adminId: number;
  reason?: string; // Max 500 characters, optional
}

/**
 * Request DTO for closing a room
 * Matches CloseRoomRequest DTO from backend
 */
export interface CloseRoomRequest {
  userId: number;
}

/**
 * Room model (enhanced with closure fields)
 */
export interface Room {
  id: number;
  name: string;
  description?: string;
  category?: string;
  joinType: "open" | "by_application";
  maxCollaborators?: number;
  isClosed: boolean;
  closedAt?: string; // ISO 8601 timestamp
  closedBy?: number; // User ID
  meetingTime?: string; // ISO 8601 timestamp
  meetingType?: "online" | "offline";
  location?: string;
  createdAt: string;
  memberCount?: number;
  // ... other room fields as needed
}

