package com.mipt.CoActivity.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found");

        // When
        ResponseEntity<Map<String, String>> response = handler.handleResourceNotFound(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody().get("message"));
        assertEquals("Resource not found", response.getBody().get("error"));
    }

    @Test
    void testHandleBadRequestException() {
        // Given
        BadRequestException exception = new BadRequestException("Bad request");

        // When
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad request", response.getBody().get("message"));
        assertEquals("Bad request", response.getBody().get("error"));
    }

    @Test
    void testHandleConflictException() {
        // Given
        ConflictException exception = new ConflictException("Conflict");

        // When
        ResponseEntity<Map<String, String>> response = handler.handleConflict(exception);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Conflict", response.getBody().get("message"));
        assertEquals("Conflict", response.getBody().get("error"));
    }

    @Test
    void testHandleForbiddenException() {
        // Given
        ForbiddenException exception = new ForbiddenException("Forbidden");

        // When
        ResponseEntity<Map<String, String>> response = handler.handleForbidden(exception);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Forbidden", response.getBody().get("message"));
        assertEquals("Forbidden", response.getBody().get("error"));
    }

    @Test
    void testHandleUnauthorizedException() {
        // Given
        UnauthorizedException exception = new UnauthorizedException("Unauthorized");

        // When
        ResponseEntity<Map<String, String>> response = handler.handleUnauthorized(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unauthorized", response.getBody().get("message"));
        assertEquals("Unauthorized", response.getBody().get("error"));
    }

    @Test
    void testHandleGenericException() {
        // Given
        Exception exception = new Exception("Generic error");

        // When
        ResponseEntity<Map<String, String>> response = handler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Generic error", response.getBody().get("message"));
        assertEquals("Generic error", response.getBody().get("error"));
        assertEquals("Exception", response.getBody().get("type"));
    }

    @Test
    void testHandleGenericException_WithNullMessage() {
        // Given
        Exception exception = new Exception() {
            @Override
            public String getMessage() {
                return null;
            }
        };

        // When
        ResponseEntity<Map<String, String>> response = handler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Exception", response.getBody().get("message"));
        assertEquals("Exception", response.getBody().get("error"));
        assertEquals("Exception", response.getBody().get("type"));
    }
}

