package com.chatapp.synk.controller;

import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.dto.UserStatusDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.security.JwtUtil;
import com.chatapp.synk.service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/all")
    public ResponseEntity<SuccessResponse<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        if (users.isEmpty()) {
            logger.warn("No users found");
            return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.NOT_FOUND, "No users found", Collections.emptyList()));
        }
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "Users fetched successfully", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserDTO>> getUserById(@PathVariable String id) {
        UserDTO userOpt = userService.getUserById(id);
        if (userOpt != null) {
            return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "User fetched", List.of(userOpt)));
        } else {
            logger.warn("User with ID {} not found", id);
            return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.NOT_FOUND, "User not found", Collections.emptyList()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserDTO>> updateUser(@PathVariable String id, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "User updated successfully", List.of(updatedUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "User deleted successfully", Collections.emptyList()));
    }

    @GetMapping("/lastActiveStatus")
    public ResponseEntity<SuccessResponse<UserStatusDTO>> getLastActiveUserStatus(@RequestParam String userId) {
        if (userId == null || userId.isEmpty()) {
            logger.warn("No user ID provided for status check");
            return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.BAD_REQUEST, "No user ID provided", Collections.emptyList()));
        }
        // this is will give the last active status of multiple users, as user can be
        // active in multiple devices, so we will return the list of status of all
        // devices
        List<UserStatusDTO> result = userService.getLastActiveUserStatus(userId);
        if (result.isEmpty()) {
            logger.warn("No status found for user ID {}", userId);
            return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.NOT_FOUND, "No status found", Collections.emptyList()));
        }
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "User statuses fetched", result));
    }

    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<UserDTO>> getUserMe(HttpServletRequest request) {
        // Get Claims directly from request attribute set by JwtAuthFilter
        // frontend only sends token. We parse it once in the filter, and set all claims in request attribute.
        Claims userDetails = (Claims) request.getAttribute("userDetails");

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SuccessResponse<>(HttpStatus.UNAUTHORIZED, "User details not found", Collections.emptyList()));
        }

        // Now extract any information without re-parsing the token
        String id = jwtUtil.extractId(userDetails);
        // List<String> roles = jwtUtil.extractRoles(userDetails);

        if (id != null) {
            UserDTO userOpt = userService.getUserById(id);
            if (userOpt != null) {
                return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "User fetched", List.of(userOpt)));
            }
        } 

        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.NOT_FOUND, "User ID not found in token", Collections.emptyList()));
    }
}
