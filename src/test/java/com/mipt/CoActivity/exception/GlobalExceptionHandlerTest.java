package com.mipt.CoActivity.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found");

        // When
        ResponseEntity<String> response = handler.handleResourceNotFound(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody());
    }

    @Test
    void testHandleBadRequestException() {
        // Given
        BadRequestException exception = new BadRequestException("Bad request");

        // When
        ResponseEntity<String> response = handler.handleBadRequest(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad request", response.getBody());
    }

    @Test
    void testHandleConflictException() {
        // Given
        ConflictException exception = new ConflictException("Conflict");

        // When
        ResponseEntity<String> response = handler.handleConflict(exception);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Conflict", response.getBody());
    }

    @Test
    void testHandleForbiddenException() {
        // Given
        ForbiddenException exception = new ForbiddenException("Forbidden");

        // When
        ResponseEntity<String> response = handler.handleForbidden(exception);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Forbidden", response.getBody());
    }

    @Test
    void testHandleUnauthorizedException() {
        // Given
        UnauthorizedException exception = new UnauthorizedException("Unauthorized");

        // When
        ResponseEntity<String> response = handler.handleUnauthorized(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unauthorized", response.getBody());
    }

    @Test
    void testHandleGenericException() {
        // Given
        Exception exception = new Exception("Generic error");

        // When
        ResponseEntity<String> response = handler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Generic error", response.getBody());
    }
}

