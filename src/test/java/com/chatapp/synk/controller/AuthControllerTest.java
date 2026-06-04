package com.chatapp.synk.controller;

import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.security.*;
import com.chatapp.synk.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private AuthDTO authDTO;
    private UserDTO userDTO;
    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        authDTO = new AuthDTO();
        authDTO.setPhoneNumberOrEmail("test@example.com");
        authDTO.setPassword("password123");

        userDTO = new UserDTO();
        userDTO.setId("user123");
        userDTO.setEmail("test@example.com");
        userDTO.setName("John Doe");
        userDTO.setPhoneNumber("9999999999");
        userDTO.setPassword("password123");

        customUserDetails = new CustomUserDetails(
            "test@example.com",
            "John Doe",
            "password123",
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            "test@example.com",
            "http://example.com/profile.jpg",
            "user123"
        );
    }

    @Test
    void testAuthenticate_Success() throws ServiceException {
        // Arrange
        Authentication mockAuth = new PhoneNumberAuthenticationToken(
            customUserDetails,
            "password123",
            customUserDetails.getAuthorities()
        );
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(jwtUtil.generateAccessToken(anyMap(), eq("test@example.com")))
            .thenReturn("jwt-token-12345");

        // Act
        ResponseEntity<JwtResponse> response = authController.authenticate(authDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("jwt-token-12345", response.getBody().getJwtToken());
        assertEquals("John Doe", response.getBody().getName());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtUtil, times(1)).generateAccessToken(anyMap(), anyString());
    }

    @Test
    void testAuthenticate_InvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> authController.authenticate(authDTO));
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void testAuthenticate_UserDisabled() {
        // Arrange
        when(authenticationManager.authenticate(any()))
            .thenThrow(new DisabledException("User account is disabled"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> authController.authenticate(authDTO));
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        UserDTO savedUser = new UserDTO();
        savedUser.setId("user123");
        savedUser.setEmail("test@example.com");
        savedUser.setName("John Doe");
        savedUser.setPhoneNumber("9999999999");

        when(userService.createUser(any(UserDTO.class))).thenReturn(savedUser);

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = authController.createUser(userDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("User created successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("********", response.getBody().getData().get(0).getPassword()); // Password masked
        verify(userService, times(1)).createUser(any(UserDTO.class));
    }

    @Test
    void testCreateUser_UnexpectedError() {
        // Arrange
        when(userService.createUser(any(UserDTO.class)))
            .thenThrow(new RuntimeException("Database connection error"));

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = authController.createUser(userDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is5xxServerError());
        assertEquals("500", response.getBody().getResponseCode());
        assertEquals("User creation failed", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(userService, times(1)).createUser(any(UserDTO.class));
    }
}
