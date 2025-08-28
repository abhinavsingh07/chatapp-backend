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

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/all")
    public ResponseEntity<SuccessResponse<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        if (users.isEmpty()) {
            logger.warn("No users found");
            return ResponseEntity.ok(new SuccessResponse<>("404", "No users found", Collections.emptyList()));
        }
        return ResponseEntity.ok(new SuccessResponse<>("200", "Users fetched successfully", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserDTO>> getUserById(@PathVariable String id) {
        UserDTO userOpt = userService.getUserById(id);
        if (userOpt != null) {
            return ResponseEntity.ok(new SuccessResponse<>("200", "User fetched", List.of(userOpt)));
        } else {
            logger.warn("User with ID {} not found", id);
            return ResponseEntity.ok(new SuccessResponse<>("404", "User not found", Collections.emptyList()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserDTO>> updateUser(@PathVariable String id, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(new SuccessResponse<>("200", "User updated successfully", List.of(updatedUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new SuccessResponse<>("200", "User deleted successfully", Collections.emptyList()));
    }

    @GetMapping("/lastActiveStatus")
    public ResponseEntity<SuccessResponse<UserStatusDTO>> getLastActiveUserStatus(@RequestParam String userId) {
        if (userId == null || userId.isEmpty()) {
            logger.warn("No user ID provided for status check");
            return ResponseEntity.ok(new SuccessResponse<>("400", "No user ID provided", Collections.emptyList()));
        }
        List<UserStatusDTO> result = userService.getLastActiveUserStatus(userId);
        if (result.isEmpty()) {
            logger.warn("No status found for user ID {}", userId);
            return ResponseEntity.ok(new SuccessResponse<>("404", "No status found", Collections.emptyList()));
        }
        return ResponseEntity.ok(new SuccessResponse<>("200", "User statuses fetched", result));
    }
}

