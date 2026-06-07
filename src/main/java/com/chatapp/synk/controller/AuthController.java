package com.chatapp.synk.controller;

import com.chatapp.synk.security_validator.InputValidationAndSanitizationService;
import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.RefreshTokenRequest;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.exceptionHandler.InvalidTokenException;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.security.*;
import com.chatapp.synk.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.chatapp.synk.response.ErrorResponse;

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

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JwtResponse> authenticate(@Valid @RequestBody AuthDTO authDTO) throws ServiceException {
        if (logger.isDebugEnabled()) {
            logger.debug("Authentication request received for identifier: {}",
                    maskIdentifier(authDTO.getPhoneNumberOrEmail()));
        }

        AuthDTO sanitizedDTO = InputValidationAndSanitizationService.validateAndSanitize(authDTO);
        Authentication auth = authenticate(sanitizedDTO.getPhoneNumberOrEmail(), sanitizedDTO.getPassword());

        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles",
                user.getAuthorities().stream().map(authObj -> authObj.getAuthority()).collect(Collectors.toList()));
        claims.put("id", user.getId());
        // dont store in jwt. In jwt only add important info like id, role etc.
        // even if user updates info we dont need to refresh jwt
        // user directly fetch latest details by id this is the main purpose to remove
        // other details
        // claims.put("email", user.getEmail());
        // claims.put("name", user.getName());//dont store in jwt

        String token = jwtUtil.generateAccessToken(claims, user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Save refresh token for later validation
        // refreshTokenService.saveRefreshToken(refreshToken, user.getUsername());

        if (logger.isDebugEnabled()) {
            logger.debug("JWT token generated for user: {}", maskIdentifier(user.getUsername()));
        }

        return ResponseEntity.ok(new JwtResponse(token, refreshToken, user.getEmail(), user.getName(),
                user.getUserRoles(), user.getEmail(), user.getProfilePictureUrl(), user.getId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            logger.info("Refresh Token request received.");
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                logger.warn("Refresh token is missing in the request");
                return ResponseEntity.status(400)
                        .body(new SuccessResponse<>("400", "Refresh token is required", List.of()));
            }

            // token validation is already doing in extractclaims, so we can directly
            // extract username and if token is invalid
            // it will throw exception and we can handle in catch block.
            String username = jwtUtil.extractUsername(refreshToken);
            if (username == null) {
                logger.warn("Refresh token validation failed - username not found");
                return ResponseEntity.status(401)
                        .body(new SuccessResponse<>("401", "Refresh token validation failed - username not found",
                                List.of()));
            }

            // Load user details
            CustomUserDetails user = (CustomUserDetails) userDetailsService.loadUserByUsername(username);

            // Generate new access token
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles",
                    user.getAuthorities().stream().map(authObj -> authObj.getAuthority()).collect(Collectors.toList()));
            claims.put("id", user.getId());

            String newToken = jwtUtil.generateAccessToken(claims, user.getUsername());

            logger.info("New JWT token generated via refresh for user: {}", maskIdentifier(user.getUsername()));

            // Return new token with existing refresh token
            return ResponseEntity.ok(new JwtResponse(newToken, refreshToken, user.getEmail(), user.getName(),
                    user.getUserRoles(), user.getEmail(), user.getProfilePictureUrl(), user.getId()));

        } catch (InvalidTokenException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(new ErrorResponse<>(HttpServletResponse.SC_UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
                            "Refresh token validation failed error msg: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse<>(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, HttpStatus.UNAUTHORIZED,
                            "Refresh token validation failed error msg: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            // String refreshToken = request.getRefreshToken();
            // refreshTokenService.revokeRefreshToken(refreshToken);
            // to do method
            logger.info("User logged out successfully");
            return ResponseEntity.ok(new SuccessResponse<>("200", "Logged out successfully", List.of()));
        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.status(500).body(new SuccessResponse<>("500", "Logout failed", List.of()));
        }
    }

    private Authentication authenticate(String username, String password) throws ServiceException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Attempting authentication for user: {}", maskIdentifier(username));
            }
            Authentication auth = authenticationManager
                    .authenticate(new PhoneNumberAuthenticationToken(username, password));

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
            savedUser.setPassword("********"); // Mask password in response

            logger.info("User registration successful. User ID: {}", savedUser.getId());
            return ResponseEntity.ok(new SuccessResponse<>("200", "User created successfully", List.of(savedUser)));
        } catch (Exception ex) {
            logger.error("Unexpected error during user creation", ex);
            return ResponseEntity.status(500).body(new SuccessResponse<>("500", "User creation failed", List.of()));
        }
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null)
            return "null";
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
