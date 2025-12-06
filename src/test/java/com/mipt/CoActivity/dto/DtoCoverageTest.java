package com.mipt.CoActivity.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests to achieve 100% class coverage for DTOs.
 * These tests verify that DTOs can be instantiated and basic operations work.
 */
class DtoCoverageTest {

    @Test
    void testLoginRequest() {
        LoginRequest dto = new LoginRequest();
        dto.setLogin("test@example.com");
        dto.setPassword("password123");
        assertEquals("test@example.com", dto.getLogin());
        assertEquals("password123", dto.getPassword());
        
        LoginRequest dto2 = new LoginRequest("test@example.com", "password123");
        assertEquals("test@example.com", dto2.getLogin());
        assertEquals("password123", dto2.getPassword());
    }

    @Test
    void testLoginResponse() {
        LoginResponse dto = new LoginResponse();
        dto.setToken("token123");
        dto.setUserId(1L);
        dto.setRequiresTwoFactor(false);
        assertEquals("token123", dto.getToken());
        assertEquals(1L, dto.getUserId());
        assertFalse(dto.getRequiresTwoFactor());
        
        LoginResponse dto2 = new LoginResponse("token123", 1L, false);
        assertEquals("token123", dto2.getToken());
        assertEquals(1L, dto2.getUserId());
    }

    @Test
    void testRegisterRequest() {
        RegisterRequest dto = new RegisterRequest();
        dto.setName("Test User");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        assertEquals("Test User", dto.getName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("password123", dto.getPassword());
    }

    @Test
    void testRegisterResponse() {
        RegisterResponse dto = new RegisterResponse();
        dto.setMessage("Success");
        dto.setUser(new com.mipt.CoActivity.model.User());
        dto.setEmailVerificationRequired(true);
        assertEquals("Success", dto.getMessage());
        assertNotNull(dto.getUser());
        assertTrue(dto.isEmailVerificationRequired());
        
        RegisterResponse dto2 = new RegisterResponse(new com.mipt.CoActivity.model.User(), true, "Success");
        assertEquals("Success", dto2.getMessage());
    }

    @Test
    void testSendMessageRequest() {
        SendMessageRequest dto = new SendMessageRequest();
        dto.setContent("Hello");
        dto.setSenderId(1L);
        dto.setImageId(2);
        assertEquals("Hello", dto.getContent());
        assertEquals(1L, dto.getSenderId());
        assertEquals(2, dto.getImageId());
        assertTrue(dto.isValid());
    }

    @Test
    void testPostRecommendationRequest() {
        PostRecommendationRequest dto = new PostRecommendationRequest();
        List<String> interests = new ArrayList<>();
        interests.add("Java");
        dto.setUserInterests(interests);
        List<PostRecommendationRequest.PostData> posts = new ArrayList<>();
        dto.setPosts(posts);
        List<Integer> subscriptions = new ArrayList<>();
        subscriptions.add(2);
        dto.setSubscribedUserIds(subscriptions);
        assertNotNull(dto.getUserInterests());
        assertNotNull(dto.getPosts());
        assertNotNull(dto.getSubscribedUserIds());
    }

    @Test
    void testPostRecommendationResponse() {
        PostRecommendationResponse dto = new PostRecommendationResponse();
        dto.setMessage("Success");
        List<PostRecommendationResponse.PostData> posts = new ArrayList<>();
        dto.setRecommendedPosts(posts);
        List<Double> scores = new ArrayList<>();
        dto.setSimilarityScores(scores);
        assertEquals("Success", dto.getMessage());
        assertNotNull(dto.getRecommendedPosts());
        assertNotNull(dto.getSimilarityScores());
    }

    @Test
    void testRecommendedPostsResponse() {
        RecommendedPostsResponse dto = new RecommendedPostsResponse();
        List<com.mipt.CoActivity.model.Post> posts = new ArrayList<>();
        dto.setPosts(posts);
        List<Double> scores = new ArrayList<>();
        dto.setSimilarityScores(scores);
        dto.setMessage("Success");
        assertNotNull(dto.getPosts());
        assertNotNull(dto.getSimilarityScores());
        assertEquals("Success", dto.getMessage());
    }

    @Test
    void testUpdateInterestsRequest() {
        UpdateInterestsRequest dto = new UpdateInterestsRequest();
        List<String> interests = new ArrayList<>();
        interests.add("Java");
        dto.setInterests(interests);
        assertNotNull(dto.getInterests());
    }

    @Test
    void testTwoFactorVerifyRequest() {
        TwoFactorVerifyRequest dto = new TwoFactorVerifyRequest();
        dto.setCode("123456");
        assertEquals("123456", dto.getCode());
    }

    @Test
    void testTwoFactorLoginRequest() {
        TwoFactorLoginRequest dto = new TwoFactorLoginRequest();
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        dto.setCode("123456");
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("password123", dto.getPassword());
        assertEquals("123456", dto.getCode());
    }

    @Test
    void testTwoFactorEnableResponse() {
        TwoFactorEnableResponse dto = new TwoFactorEnableResponse();
        dto.setSecret("secret123");
        dto.setQrCodeUrl("qr123");
        dto.setManualEntryKey("key123");
        assertEquals("secret123", dto.getSecret());
        assertEquals("qr123", dto.getQrCodeUrl());
        assertEquals("key123", dto.getManualEntryKey());
        
        TwoFactorEnableResponse dto2 = new TwoFactorEnableResponse("secret123", "qr123", "key123");
        assertEquals("secret123", dto2.getSecret());
    }

    @Test
    void testPasswordResetRequest() {
        PasswordResetRequest dto = new PasswordResetRequest();
        dto.setEmail("test@example.com");
        assertEquals("test@example.com", dto.getEmail());
    }

    @Test
    void testPasswordResetConfirmRequest() {
        PasswordResetConfirmRequest dto = new PasswordResetConfirmRequest();
        dto.setToken("token123");
        dto.setNewPassword("newpass123");
        assertEquals("token123", dto.getToken());
        assertEquals("newpass123", dto.getNewPassword());
    }

    @Test
    void testNotificationData() {
        NotificationData dto = new NotificationData();
        dto.setRoomId(1L);
        dto.setRequestId(2L);
        dto.setRequesterId(3L);
        assertEquals(1L, dto.getRoomId());
        assertEquals(2L, dto.getRequestId());
        assertEquals(3L, dto.getRequesterId());
        
        NotificationData dto2 = NotificationData.builder()
                .roomId(1L)
                .requestId(2L)
                .build();
        assertEquals(1L, dto2.getRoomId());
        assertEquals(2L, dto2.getRequestId());
    }

    @Test
    void testRoomJoinRequestResponse() {
        RoomJoinRequestResponse dto = new RoomJoinRequestResponse();
        dto.setId(1L);
        dto.setStatus("PENDING");
        assertEquals(1L, dto.getId());
        assertEquals("PENDING", dto.getStatus());
    }

    @Test
    void testRoomDetailsResponse() {
        RoomDetailsResponse dto = new RoomDetailsResponse();
        dto.setId(1L);
        dto.setName("Test Room");
        assertEquals(1L, dto.getId());
        assertEquals("Test Room", dto.getName());
    }

    @Test
    void testRejectJoinRequestRequest() {
        RejectJoinRequestRequest dto = new RejectJoinRequestRequest();
        dto.setReason("Not suitable");
        dto.setAdminId(1L);
        assertEquals("Not suitable", dto.getReason());
        assertEquals(1L, dto.getAdminId());
    }

    @Test
    void testMembershipRequestRequest() {
        MembershipRequestRequest dto = new MembershipRequestRequest();
        dto.setMessage("Please accept");
        assertEquals("Please accept", dto.getMessage());
    }

    @Test
    void testCreateRoomRequest() {
        CreateRoomRequest dto = new CreateRoomRequest();
        dto.setDescription("Description");
        dto.setCategory("Java");
        dto.setMaxCollaborators(10);
        dto.setMeetingType("online");
        dto.setJoinType("open");
        assertEquals("Description", dto.getDescription());
        assertEquals("Java", dto.getCategory());
        assertEquals(10, dto.getMaxCollaborators());
        assertEquals("online", dto.getMeetingType());
        assertEquals("open", dto.getJoinType());
    }

    @Test
    void testCreateRoomJoinRequestRequest() {
        CreateRoomJoinRequestRequest dto = new CreateRoomJoinRequestRequest();
        dto.setMessage("Please accept");
        assertEquals("Please accept", dto.getMessage());
    }
}
