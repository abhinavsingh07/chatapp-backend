package com.chatapp.synk.controller;

import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    //Basic Setup with SLF4J logger
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<UserDTO>> searchUsers(
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String email) {
        logger.info("Received search request with query parameters - Phone Number: {}, Email: {}", phoneNumber, email);

        List<UserDTO> results = userService.searchUsers(phoneNumber, email);
        if (results.isEmpty()) {
            logger.warn("No users found ");
        } else {
            logger.info("Found {} users", results.size());
        }

        String msg = results.isEmpty() ? "No matching users found" : "Search results";
        String code = results.isEmpty() ? "404" : "200";

        return ResponseEntity.ok(new SuccessResponse<>(code, msg, results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserDTO>> getUserById(@PathVariable String id) {
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

    @GetMapping("/all")
    public ResponseEntity<SuccessResponse<UserDTO>> getAllUsers(){
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

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserDTO>> updateUser(@PathVariable String id, @RequestBody UserDTO userDTO) {
        logger.info("Received update request for user ID: {}", id);

        UserDTO updatedUser = userService.updateUser(id, userDTO);
        logger.info("User ID {} updated successfully", id);

        return ResponseEntity.ok(new SuccessResponse<>("200", "User updated successfully", List.of(updatedUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse<Void>> deleteUser(@PathVariable String id) {
        logger.info("Received delete request for user ID: {}", id);

        userService.deleteUser(id);
        logger.info("User ID {} deleted successfully", id);

        return ResponseEntity.ok(new SuccessResponse<>("200", "User deleted successfully", Collections.emptyList()));
    }
}
