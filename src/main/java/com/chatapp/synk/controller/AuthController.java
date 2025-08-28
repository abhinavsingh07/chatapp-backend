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

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JwtResponse> authenticate(@Valid @RequestBody AuthDTO authDTO) throws ServiceException {
        if (logger.isDebugEnabled()) {
            logger.debug("Authentication request received for identifier: {}", maskIdentifier(authDTO.getPhoneNumberOrEmail()));
        }

        AuthDTO sanitizedDTO = InputValidationAndSanitizationService.validateAndSanitize(authDTO);
        Authentication auth = authenticate(sanitizedDTO.getPhoneNumberOrEmail(), sanitizedDTO.getPassword());

        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getAuthorities().stream().map(authObj -> authObj.getAuthority()).collect(Collectors.toList()));
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());

        String token = jwtUtil.generateToken(claims, user.getUsername());

        if (logger.isDebugEnabled()) {
            logger.debug("JWT token generated for user: {}", maskIdentifier(user.getUsername()));
        }

        return ResponseEntity.ok(new JwtResponse(token, user.getEmail(), user.getName(), user.getUserRoles(), user.getEmail(), user.getProfilePictureUrl(), user.getId()));
    }

    private Authentication authenticate(String username, String password) throws ServiceException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Attempting authentication for user: {}", maskIdentifier(username));
            }
            Authentication auth = authenticationManager.authenticate(new PhoneNumberAuthenticationToken(username, password));

            if (logger.isDebugEnabled()) {
                logger.debug("Authentication successful for user: {}", maskIdentifier(username));
            }
            return auth;
        } catch (DisabledException e) {
            logger.warn("Authentication failed - account disabled for user: {}", maskIdentifier(username));
            throw new ServiceException("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed - invalid credentials for user: {}", maskIdentifier(username));
            throw new ServiceException("INVALID_CREDENTIALS", e);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<UserDTO>> createUser(@Valid @RequestBody UserDTO userDTO) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering new user with identifier: {}", maskIdentifier(userDTO.getPhoneNumber()));
            }
            UserDTO savedUser = userService.createUser(userDTO);
            savedUser.setPassword("********");  // Mask password in response

            logger.info("User registration successful. User ID: {}", savedUser.getId());
            return ResponseEntity.ok(new SuccessResponse<>("200", "User created successfully", List.of(savedUser)));
        } catch (Exception ex) {
            logger.error("Unexpected error during user creation", ex);
            return ResponseEntity.status(500).body(new SuccessResponse<>("500", "User creation failed", List.of()));
        }
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null) return "null";
        // mask email/phone for logs: e.g., 98****1234 or j***@domain.com
        if (identifier.contains("@")) {
            int idx = identifier.indexOf("@");
            return identifier.charAt(0) + "***" + identifier.substring(idx);
        } else if (identifier.length() > 4) {
            return identifier.substring(0, 2) + "****" + identifier.substring(identifier.length() - 2);
        }
        return "****";
    }
}
