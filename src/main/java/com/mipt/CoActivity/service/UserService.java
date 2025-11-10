package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Autowired
  UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User getUserByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public User getUserByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public boolean isUsernameExists(String username) {
    return userRepository.findByUsername(username) != null;
  }

  public boolean isEmailExists(String email) {
    return userRepository.findByEmail(email) != null;
  }

  private String hashPassword(String password) {
    return passwordEncoder.encode(password);
  }

  public User registerUser(String username, String email, String password) {
    if (isUsernameExists(username)) {
      throw new RuntimeException("Username already exists");
    }

    if (isEmailExists(email)) {
      throw new RuntimeException("Email already exists");
    }

    String hashedPassword = hashPassword(password);

    User newUser = new User(username, email, hashedPassword);

    return userRepository.save(newUser);
  }

  public void subscribe(Long userId, Long userToSubscribeId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    User userToSubscribe =
        userRepository
            .findById(userToSubscribeId)
            .orElseThrow(() -> new RuntimeException("User to subscribe not found"));

    if (user.getSubscriptions().contains(userToSubscribe)) {
      throw new RuntimeException("User is already subscribed to this user");
    }

    user.getSubscriptions().add(userToSubscribe);
    userToSubscribe.getFollowers().add(user);

    userRepository.save(user);
    userRepository.save(userToSubscribe);
  }

  public void unsubscribe(Long userId, Long userToUnsubscribeId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    User userToUnsubscribe =
        userRepository
            .findById(userToUnsubscribeId)
            .orElseThrow(() -> new RuntimeException("User to unsubscribe not found"));

    if (!user.getSubscriptions().contains(userToUnsubscribe)) {
      throw new RuntimeException("User is not subscribed to this user");
    }

    user.getSubscriptions().remove(userToUnsubscribe);
    userToUnsubscribe.getFollowers().remove(user);

    userRepository.save(user);
    userRepository.save(userToUnsubscribe);
  }
}
