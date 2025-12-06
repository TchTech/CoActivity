package com.mipt.CoActivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @Autowired
    private ObjectMapper objectMapper;

    private Post testPost;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@test.com", "password");
        testUser.setId(1L);
        
        testPost = new Post();
        testPost.setId(1);
        testPost.setName("Test Post");
        testPost.setText("Test content");
        testPost.setAuthor(testUser);
    }

    @Test
    void testCreatePost_Success() throws Exception {
        // Given
        when(postService.createPost(any(Post.class))).thenReturn(testPost);

        // When & Then
        mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPost)))
                .andExpect(status().isCreated());
    }
}

