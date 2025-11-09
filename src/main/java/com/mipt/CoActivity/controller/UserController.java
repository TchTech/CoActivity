package com.mipt.CoActivity.controller;

import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public User registerUser(
      @RequestParam String username, @RequestParam String email, @RequestParam String password) {
    return userService.registerUser(username, email, password);
  }

  @PostMapping("/subscribe")
  public void subscribe(@RequestParam Long userId, @RequestParam Long userToSubscribeId) {
    userService.subscribe(userId, userToSubscribeId);
  }

  public void unsubscribe(@RequestParam Long userId, @RequestParam Long userToUnsubscribeId) {
    userService.unsubscribe(userId, userToUnsubscribeId);
  }
}
