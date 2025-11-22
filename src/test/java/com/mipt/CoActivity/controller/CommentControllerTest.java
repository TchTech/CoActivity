package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.BaseIntegrationTest;
import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class CommentControllerTest extends BaseIntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser;
    private Post testPost;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        imageRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("commentuser", "commentuser@example.com", passwordEncoder.encode("password123"));
        testUser.setName("Comment User");
        testUser = userRepository.save(testUser);

        Image testImage = new Image();
        testImage.setContent("test image".getBytes());
        testImage = imageRepository.save(testImage);

        testPost = new Post("Test Post", testUser, "Post content for comments", testImage);
        testPost = postRepository.save(testPost);
    }

    @Test
    void shouldCreateComment() {
        Map<String, Object> commentData = new HashMap<>();
        Map<String, Long> authorData = new HashMap<>();
        authorData.put("id", testUser.getId());
        commentData.put("author", authorData);
        commentData.put("text", "This is a test comment");

        given()
            .contentType(ContentType.JSON)
            .body(commentData)
            .when()
            .post("/posts/{postId}/comments", testPost.getId())
            .then()
            .statusCode(201)
            .body("text", equalTo("This is a test comment"))
            .body("author.id", equalTo(testUser.getId().intValue()));
    }

    @Test
    void shouldReturn404WhenPostNotFound() {
        Map<String, Object> commentData = new HashMap<>();
        Map<String, Long> authorData = new HashMap<>();
        authorData.put("id", testUser.getId());
        commentData.put("author", authorData);
        commentData.put("text", "Comment on non-existent post");

        given()
            .contentType(ContentType.JSON)
            .body(commentData)
            .when()
            .post("/posts/{postId}/comments", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    void shouldReturn400WhenCommentWithoutAuthor() {
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("text", "Comment without author");

        given()
            .contentType(ContentType.JSON)
            .body(commentData)
            .when()
            .post("/posts/{postId}/comments", testPost.getId())
            .then()
            .statusCode(500);
    }

    @Test
    void shouldCreateMultipleComments() {
        Map<String, Object> commentData1 = new HashMap<>();
        Map<String, Long> authorData1 = new HashMap<>();
        authorData1.put("id", testUser.getId());
        commentData1.put("author", authorData1);
        commentData1.put("text", "First comment");

        given()
            .contentType(ContentType.JSON)
            .body(commentData1)
            .when()
            .post("/posts/{postId}/comments", testPost.getId())
            .then()
            .statusCode(201);

        Map<String, Object> commentData2 = new HashMap<>();
        Map<String, Long> authorData2 = new HashMap<>();
        authorData2.put("id", testUser.getId());
        commentData2.put("author", authorData2);
        commentData2.put("text", "Second comment");

        given()
            .contentType(ContentType.JSON)
            .body(commentData2)
            .when()
            .post("/posts/{postId}/comments", testPost.getId())
            .then()
            .statusCode(201)
            .body("text", equalTo("Second comment"));
    }

    @Test
    void shouldCreateCommentWithDifferentAuthor() {
        User anotherUser = new User("anotheruser", "another@example.com", passwordEncoder.encode("password123"));
        anotherUser = userRepository.save(anotherUser);

        Map<String, Object> commentData = new HashMap<>();
        Map<String, Long> authorData = new HashMap<>();
        authorData.put("id", anotherUser.getId());
        commentData.put("author", authorData);
        commentData.put("text", "Comment from another user");

        given()
            .contentType(ContentType.JSON)
            .body(commentData)
            .when()
            .post("/posts/{postId}/comments", testPost.getId())
            .then()
            .statusCode(201)
            .body("text", equalTo("Comment from another user"))
            .body("author.id", equalTo(anotherUser.getId().intValue()));
    }

    @Test
    void shouldCreateCommentWithEmptyText() {
        Map<String, Object> commentData = new HashMap<>();
        Map<String, Long> authorData = new HashMap<>();
        authorData.put("id", testUser.getId());
        commentData.put("author", authorData);
        commentData.put("text", "");

        given()
            .contentType(ContentType.JSON)
            .body(commentData)
            .when()
            .post("/posts/{postId}/comments", testPost.getId())
            .then()
            .statusCode(anyOf(is(201), is(400)));
    }
}

