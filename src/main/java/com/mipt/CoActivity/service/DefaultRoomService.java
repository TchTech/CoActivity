package com.mipt.CoActivity.service;

import com.mipt.CoActivity.model.Room;
import com.mipt.CoActivity.model.User;
import com.mipt.CoActivity.repository.RoomRepository;
import com.mipt.CoActivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Order(1) // Run early in startup
public class DefaultRoomService implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DefaultRoomService.class);
    
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public DefaultRoomService(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }
    
    @Override
    @Transactional
    public void run(String... args) {
        initializeDefaultRoom();
    }
    
    @Transactional
    public void initializeDefaultRoom() {
        // Check if default room exists
        Room defaultRoom = roomRepository.findByIsDefaultTrue().orElse(null);
        
        if (defaultRoom == null) {
            // Create default room
            List<User> allUsers = userRepository.findAll();
            User firstUser = allUsers.isEmpty() ? null : allUsers.get(0);
            
            if (firstUser == null) {
                logger.warn("No users found, cannot create default room. Will be created when first user registers.");
                return;
            }
            
            defaultRoom = new Room(firstUser, "General");
            defaultRoom.setDescription("Default room for all users");
            defaultRoom.setCategory("General");
            defaultRoom.setIsDefault(true);
            defaultRoom.setJoinType("open");
            defaultRoom = roomRepository.save(defaultRoom);
            
            // Add creator as admin
            defaultRoom.getAdmins().add(firstUser);
            
            // Add all existing users as members
            for (User user : allUsers) {
                if (!defaultRoom.getCollaborators().contains(user)) {
                    defaultRoom.getCollaborators().add(user);
                }
            }
            
            roomRepository.save(defaultRoom);
            logger.info("Default room created with {} members", defaultRoom.getCollaborators().size());
        } else {
            // Ensure all existing users are members
            List<User> allUsers = userRepository.findAll();
            boolean updated = false;
            
            for (User user : allUsers) {
                if (!defaultRoom.getCollaborators().contains(user)) {
                    defaultRoom.getCollaborators().add(user);
                    updated = true;
                }
            }
            
            if (updated) {
                roomRepository.save(defaultRoom);
                logger.info("Default room updated, now has {} members", defaultRoom.getCollaborators().size());
            }
        }
    }
}

