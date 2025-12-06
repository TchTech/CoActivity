package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.model.Comment;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CommentController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private Comment testComment;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@test.com", "password");
        testUser.setId(1L);
        
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setText("Test comment");
        testComment.setAuthor(testUser);
    }

    @Test
    void testCreateComment_Success() throws Exception {
        // Given
        when(commentService.createComment(eq(1L), any(Comment.class))).thenReturn(testComment);

        // When & Then
        mockMvc.perform(post("/posts/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Test comment\",\"author\":{\"id\":1}}"))
                .andExpect(status().isCreated());
    }
}

