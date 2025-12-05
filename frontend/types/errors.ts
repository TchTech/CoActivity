/**
 * Type definitions for API errors
 */

/**
 * API Error response structure
 */
export interface ApiError {
  message: string;
  status: number;
  data?: Record<string, string>; // For validation errors (field -> error message)
}

/**
 * Common error messages from backend
 * These match the error messages returned by the backend
 */
export const ErrorMessages = {
  ROOM_OPEN_TYPE: "Requests can only be created for rooms with joinType 'by_application'",
  ALREADY_MEMBER: "User is already a member of this room",
  PENDING_REQUEST_EXISTS: "You have already applied to this room",
  COOLDOWN_ACTIVE: "You must wait",
  ROOM_AT_CAPACITY: "Room has reached maximum capacity",
  NOT_ADMIN: "Only room creator or administrators can",
  NOT_REQUEST_OWNER: "You can only cancel your own requests",
  REQUEST_NOT_PENDING: "Only pending requests can be",
  ROOM_ALREADY_CLOSED: "Room is already closed",
} as const;

/**
 * Handle API errors and extract user-friendly messages
 * 
 * @param error - The error object from API call
 * @returns User-friendly error message
 */
export function handleApiError(error: any): string {
  // Check if it's a validation error (400 with field errors)
  if (error.status === 400) {
    if (error.data && typeof error.data === "object") {
      // Extract field error messages
      const messages = Object.values(error.data).join(", ");
      return messages;
    }
    return error.message || "Invalid request";
  }
  
  if (error.status === 403) {
    return "You don't have permission to perform this action";
  }
  
  if (error.status === 404) {
    return "Resource not found";
  }
  
  if (error.status === 409) {
    // Check for specific conflict messages
    const message = error.message || "";
    if (message.includes("already a member")) {
      return ErrorMessages.ALREADY_MEMBER;
    }
    if (message.includes("already applied")) {
      return ErrorMessages.PENDING_REQUEST_EXISTS;
    }
    if (message.includes("capacity")) {
      return ErrorMessages.ROOM_AT_CAPACITY;
    }
    return message || "Conflict occurred";
  }
  
  return error.message || "An error occurred";
}

/**
 * Extract cooldown minutes from error message
 * 
 * @param error - The error object
 * @returns Number of minutes remaining in cooldown, or null if not a cooldown error
 */
export function extractCooldownMinutes(error: any): number | null {
  const message = error.message || "";
  const match = message.match(/(\d+)\s+more\s+minute/i);
  return match ? parseInt(match[1], 10) : null;
}

/**
 * Check if error is a cooldown error
 */
export function isCooldownError(error: any): boolean {
  return error.message?.includes(ErrorMessages.COOLDOWN_ACTIVE) || false;
}

