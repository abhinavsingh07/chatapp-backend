package com.chatapp.synk.controller;

import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.dto.UserStatusDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    //Basic Setup with SLF4J logger
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/all")
    public ResponseEntity<SuccessResponse<UserDTO>> getAllUsers() {
        logger.info("Fetching all users");
        List<UserDTO> users = userService.getAllUsers();
        if (users.isEmpty()) {
            logger.warn("No users found");
            return ResponseEntity.ok(new SuccessResponse<>("404", "No users found", Collections.emptyList()));
        } else {
            logger.info("Found {} users", users.size());
            return ResponseEntity.ok(new SuccessResponse<>("200", "Users fetched successfully", users));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserDTO>> getUserById(@PathVariable(required = true) String id) {
        logger.info("Fetching user with ID: {}", id);

        UserDTO userOpt = userService.getUserById(id);
        if (userOpt != null) {
            logger.info("User with ID {} found", id);
            return ResponseEntity.ok(new SuccessResponse<>("200", "User fetched", List.of(userOpt)));
        } else {
            logger.warn("User with ID {} not found", id);
            return ResponseEntity.ok(new SuccessResponse<>("404", "User not found", Collections.emptyList()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserDTO>> updateUser(@PathVariable(required = true) String id, @RequestBody UserDTO userDTO) {
        logger.info("Received update request for user ID: {}", id);

        UserDTO updatedUser = userService.updateUser(id, userDTO);
        logger.info("User ID {} updated successfully", id);

        return ResponseEntity.ok(new SuccessResponse<>("200", "User updated successfully", List.of(updatedUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse<Void>> deleteUser(@PathVariable(required = true) String id) {
        logger.info("Received delete request for user ID: {}", id);

        userService.deleteUser(id);
        logger.info("User ID {} deleted successfully", id);

        return ResponseEntity.ok(new SuccessResponse<>("200", "User deleted successfully", Collections.emptyList()));
    }

    @GetMapping("/lastActiveStatus")
    public ResponseEntity<SuccessResponse<UserStatusDTO>> getLastActiveUserStatus(@RequestParam(name = "userId") String userId) {
        if (userId == null || userId.isEmpty()) {
            logger.warn("No user IDs provided for status check");
            return ResponseEntity.ok(new SuccessResponse<>("400", "No user IDs provided", Collections.emptyList()));
        }
        logger.info("Fetching last active status for user IDs: {}", userId);
        List<UserStatusDTO> result = userService.getLastActiveUserStatus(userId);
        if (result.isEmpty()) {
            logger.warn("No status found for provided user IDs");
            return ResponseEntity.ok(new SuccessResponse<>("404", "No status found for provided user IDs", Collections.emptyList()));
        }
        return ResponseEntity.ok(new SuccessResponse<>("200", "User statuses fetched", result));
    }
}
