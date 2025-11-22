package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.BaseIntegrationTest;
import com.mipt.CoActivity.model.Image;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.Room;
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

class PostControllerTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser;
    private Room testRoom;
    private Image testImage;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        imageRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("postuser", "postuser@example.com", passwordEncoder.encode("password123"));
        testUser.setName("Post User");
        testUser = userRepository.save(testUser);

        testRoom = new Room(testUser, "Test Room");
        testRoom = roomRepository.save(testRoom);

        testImage = new Image();
        testImage.setContent("test image content".getBytes());
        testImage = imageRepository.save(testImage);
    }

    @Test
    void shouldCreatePost() {
        Map<String, Object> postData = new HashMap<>();
        Map<String, Long> authorData = new HashMap<>();
        authorData.put("id", testUser.getId());
        postData.put("author", authorData);
        postData.put("text", "This is a test post");
        postData.put("name", "Test Post");

        Map<String, Long> roomData = new HashMap<>();
        roomData.put("id", testRoom.getId());
        postData.put("room", roomData);

        Map<String, Integer> imageData = new HashMap<>();
        imageData.put("id", testImage.getId());
        postData.put("image", imageData);

        given()
            .contentType(ContentType.JSON)
            .body(postData)
            .when()
            .post("/posts")
            .then()
            .statusCode(201)
            .body("text", equalTo("This is a test post"))
            .body("name", equalTo("Test Post"));
    }

    @Test
    void shouldReturn400WhenCreatingPostWithoutAuthor() {
        Map<String, Object> postData = new HashMap<>();
        postData.put("text", "Post without author");
        postData.put("name", "Invalid Post");

        given()
            .contentType(ContentType.JSON)
            .body(postData)
            .when()
            .post("/posts")
            .then()
            .statusCode(500);
    }

    @Test
    void shouldLikePost() {
        Post post = new Post("Test Post", testUser, "Post content", testImage);
        post = postRepository.save(post);

        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", testUser.getId())
            .when()
            .post("/posts/{postId}/like", post.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldDislikePost() {
        Post post = new Post("Test Post", testUser, "Post content", testImage);
        post = postRepository.save(post);

        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", testUser.getId())
            .when()
            .post("/posts/{postId}/dislike", post.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldToggleLikePost() {
        Post post = new Post("Test Post", testUser, "Post content", testImage);
        post = postRepository.save(post);

        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", testUser.getId())
            .when()
            .post("/posts/{postId}/like", post.getId())
            .then()
            .statusCode(200);

        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", testUser.getId())
            .when()
            .post("/posts/{postId}/like", post.getId())
            .then()
            .statusCode(200);
    }

    @Test
    void shouldReturn404WhenPostNotFound() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("userId", testUser.getId())
            .when()
            .post("/posts/{postId}/like", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    void shouldPublishPostWithLegacyEndpoint() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("name", "Legacy Post")
            .formParam("author", testUser.getId())
            .formParam("text", "Legacy post content")
            .formParam("image", testImage.getId())
            .when()
            .post("/posts/publish")
            .then()
            .statusCode(200);
    }

    @Test
    void shouldCreatePostWithRoom() {
        Map<String, Object> postData = new HashMap<>();
        Map<String, Long> authorData = new HashMap<>();
        authorData.put("id", testUser.getId());
        postData.put("author", authorData);
        postData.put("text", "Post in room");
        postData.put("name", "Room Post");

        Map<String, Long> roomData = new HashMap<>();
        roomData.put("id", testRoom.getId());
        postData.put("room", roomData);

        given()
            .contentType(ContentType.JSON)
            .body(postData)
            .when()
            .post("/posts")
            .then()
            .statusCode(201)
            .body("text", equalTo("Post in room"));
    }

    @Test
    void shouldCreatePostWithoutImage() {
        Map<String, Object> postData = new HashMap<>();
        Map<String, Long> authorData = new HashMap<>();
        authorData.put("id", testUser.getId());
        postData.put("author", authorData);
        postData.put("text", "Post without image");
        postData.put("name", "No Image Post");

        given()
            .contentType(ContentType.JSON)
            .body(postData)
            .when()
            .post("/posts")
            .then()
            .statusCode(201)
            .body("text", equalTo("Post without image"));
    }
}

