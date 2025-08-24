package com.chatapp.synk.controller;

import com.chatapp.synk.security_validator.InputValidationAndSanitizationService;
import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.security.*;
import com.chatapp.synk.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private AuthenticationManager authenticationManager;

    private JwtUtil jwtUtil;

    private CustomUserDetailsService userDetailsService;

    private UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JwtResponse> authenticate(@Valid @RequestBody AuthDTO authDTO) throws ServiceException {
        logger.info("Authenticating user with phone number: {}", authDTO.getPhoneNumberOrEmail());
        AuthDTO sanitizedDTO = InputValidationAndSanitizationService.validateAndSanitize(authDTO);
        //authenticate user using phone number or email and password
        Authentication auth = authenticate(sanitizedDTO.getPhoneNumberOrEmail(), sanitizedDTO.getPassword());
        //token generation flow
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();//it has userdetails called in phonenoauthprovder by constructor we are setting when user verfies.
        // Add all required claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getAuthorities().stream().map(authObj -> authObj.getAuthority()).collect(Collectors.toList()));
        claims.put("id", user.getId());
        //claims.put("name", user.getName());
        claims.put("email", user.getEmail());
        // claims.put("profilePictureUrl", user.getProfilePictureUrl());

        // Generate JWT token
        String token = jwtUtil.generateToken(claims, user.getUsername());
        logger.info("JWT token generated successfully for user: {}", sanitizedDTO.getPhoneNumberOrEmail());
        return ResponseEntity.ok(new JwtResponse(token, user.getEmail(), user.getName(), user.getUserRoles(), user.getEmail(), user.getProfilePictureUrl(), user.getId()));
    }

    private Authentication authenticate(String username, String password) throws ServiceException {
        try {
            logger.info("Attempting authentication for user: {}", username);
            //internally call our custom PhoneNumberAuthenticationProvider authenticate method
            Authentication auth = authenticationManager.authenticate(new PhoneNumberAuthenticationToken(username, password));
            logger.info("Authentication successful for user: {}", username);
            return auth;
        } catch (DisabledException e) {
            logger.error("User account is disabled: {}", username);
            throw new ServiceException("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            logger.error("Invalid credentials for user: {}", username);
            throw new ServiceException("INVALID_CREDENTIALS", e);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<UserDTO>> createUser(@Valid @RequestBody UserDTO userDTO) {
        try {
            logger.info("Registering new user with phone number: {}", userDTO.getPhoneNumber());
            UserDTO savedUser = userService.createUser(userDTO);
            savedUser.setPassword("********");  // Mask password in response
            logger.info("User registration successful. User ID: {}", savedUser.getId());
            return ResponseEntity.ok(new SuccessResponse<>("200", "User created successfully", List.of(savedUser)));
        } catch (Exception ex) {
            logger.error("User creation failed: {}", ex.getMessage());
        }
        return ResponseEntity.status(500).body(new SuccessResponse<>("500", "User creation failed", List.of()));
    }
}