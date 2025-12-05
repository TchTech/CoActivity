/**
 * Type definitions for Notification system
 */

/**
 * Notification type constants
 * Matches notification types from backend
 */
export type NotificationType =
  | "MEMBERSHIP_REQUEST"
  | "MEMBERSHIP_APPROVED"
  | "MEMBERSHIP_REJECTED"
  | "MEMBERSHIP_CANCELLED"
  | "ROOM_CLOSED"
  | "ROOM_CAPACITY_REACHED"
  | "USER_BANNED"
  | "POST_PINNED"
  | "COMMENT"
  | "MENTION"
  | "EVENT_REMINDER_1H"
  | "EVENT_REMINDER_24H"
  | "NEW_POST"
  | "POST_LIKED"
  | "POST_DISLIKED"
  | "POST_COMMENTED"
  | "COMMENT_REPLY"
  | "FOLLOW";

/**
 * Standardized notification data structure
 * Matches NotificationData DTO from backend
 */
export interface NotificationData {
  roomId?: number;
  requestId?: number;
  requesterId?: number;
  responderId?: number;
  reason?: string;
  postId?: number;
  commentId?: number;
  authorId?: number;
  authorName?: string;
  likerId?: number;
  likerName?: string;
  commenterId?: number;
  commenterName?: string;
  subscriberId?: number;
  subscriberName?: string;
  parentCommentId?: number;
}

/**
 * Notification model from API
 * Matches Notification entity from backend
 */
export interface Notification {
  id: number;
  userId: number;
  type: NotificationType;
  title: string;
  content: string;
  data?: string; // JSON string of NotificationData
  isRead: boolean;
  createdAt: string; // ISO 8601 timestamp
  deduplicationHash?: string;
}

/**
 * Helper function to parse notification data from JSON string
 * 
 * @param notification - The notification object
 * @returns Parsed NotificationData or null if parsing fails
 */
export function parseNotificationData(notification: Notification): NotificationData | null {
  if (!notification.data) return null;
  try {
    return JSON.parse(notification.data) as NotificationData;
  } catch (error) {
    console.error("Failed to parse notification data:", error);
    return null;
  }
}

/**
 * Check if notification is a membership request type (for room owners/admins)
 */
export function isMembershipRequestType(type: NotificationType): boolean {
  return type === "MEMBERSHIP_REQUEST";
}

/**
 * Check if notification is a membership decision type (for applicants)
 */
export function isMembershipDecisionType(type: NotificationType): boolean {
  return [
    "MEMBERSHIP_APPROVED",
    "MEMBERSHIP_REJECTED",
    "MEMBERSHIP_CANCELLED",
    "ROOM_CLOSED",
    "ROOM_CAPACITY_REACHED",
    "USER_BANNED",
  ].includes(type);
}

