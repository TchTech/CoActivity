package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.BaseIntegrationTest;
import com.mipt.CoActivity.model.Interest;
import com.mipt.CoActivity.model.InterestCategory;
import com.mipt.CoActivity.model.Post;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.PostRepository;
import com.mipt.CoActivity.repository.UserRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Disabled;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Disabled("Integration tests are slow - use unit tests instead")
class PostControllerRecommendationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser;
    private User subscribedUser;
    private Post testPost1;
    private Post testPost2;
    private Interest interest1;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем тестового пользователя
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        testUser.setName("Test User");
        testUser = userRepository.save(testUser);

        // Создаем пользователя для подписки
        subscribedUser = new User("subscribed", "subscribed@example.com", passwordEncoder.encode("password123"));
        subscribedUser.setName("Subscribed User");
        subscribedUser = userRepository.save(subscribedUser);

        // Создаем интересы
        InterestCategory category = new InterestCategory();
        category.setId(1);
        category.setName("IT");

        interest1 = new Interest();
        interest1.setId(1);
        interest1.setName("Программирование");
        interest1.setCategory(category);

        testUser.setInterests(Arrays.asList(interest1));
        testUser = userRepository.save(testUser);

        // Создаем посты
        testPost1 = new Post();
        testPost1.setName("Post about Programming");
        testPost1.setText("This is a post about programming and Python");
        testPost1.setAuthor(testUser);
        testPost1 = postRepository.save(testPost1);

        testPost2 = new Post();
        testPost2.setName("Post from subscribed user");
        testPost2.setText("This is a post from a subscribed user about Java");
        testPost2.setAuthor(subscribedUser);
        testPost2 = postRepository.save(testPost2);
    }

    @Test
    void shouldGetRecommendedPosts_WithInterests() {
        given()
            .param("userId", testUser.getId())
            .when()
            .get("/posts/recommended")
            .then()
            .statusCode(200)
            .body("$", is(not(empty())))
            .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    void shouldGetRecommendedPosts_WithSubscriptions() {
        // Подписываем пользователя на другого пользователя
        testUser.setSubscriptions(Arrays.asList(subscribedUser));
        testUser = userRepository.save(testUser);

        given()
            .param("userId", testUser.getId())
            .when()
            .get("/posts/recommended")
            .then()
            .statusCode(200)
            .body("$", is(not(empty())))
            .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    void shouldReturnEmptyList_WhenUserNotFound() {
        given()
            .param("userId", 99999L)
            .when()
            .get("/posts/recommended")
            .then()
            .statusCode(404)
            .body("$", is(empty()));
    }

    @Test
    void shouldReturnPosts_WhenNoInterests() {
        // Удаляем интересы у пользователя
        testUser.setInterests(new ArrayList<>());
        testUser.setSubscriptions(new ArrayList<>());
        testUser = userRepository.save(testUser);

        given()
            .param("userId", testUser.getId())
            .when()
            .get("/posts/recommended")
            .then()
            .statusCode(200)
            .body("$", is(not(empty())))
            .body("size()", greaterThanOrEqualTo(2)); // Должны вернуться все посты
    }

    @Test
    void shouldReturnEmptyList_WhenNoPosts() {
        // Удаляем все посты
        postRepository.deleteAll();

        given()
            .param("userId", testUser.getId())
            .when()
            .get("/posts/recommended")
            .then()
            .statusCode(200)
            .body("$", is(empty()));
    }
}

