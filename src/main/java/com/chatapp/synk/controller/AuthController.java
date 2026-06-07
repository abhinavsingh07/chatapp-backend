package com.chatapp.synk.controller;

import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.RefreshTokenRequest;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.security.JwtResponse;
import com.chatapp.synk.service.UserService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JwtResponse> authenticate(@Valid @RequestBody AuthDTO authDTO) {
        return ResponseEntity.ok(userService.authenticate(authDTO));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Refresh Token request received.");
        return ResponseEntity.ok(userService.refreshToken(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<SuccessResponse<?>> forgotPassword(@Valid @RequestBody AuthDTO authDTO) {
        userService.forgotPassword(authDTO);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "Password updated successfully", List.of()));
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<UserDTO>> createUser(@Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(new SuccessResponse<>(
                HttpStatus.OK,
                "User created successfully",
                List.of(userService.registerUser(userDTO))));
    }
}
