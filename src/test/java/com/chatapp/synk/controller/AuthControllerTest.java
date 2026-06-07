package com.chatapp.synk.controller;

import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.security.JwtResponse;
import com.chatapp.synk.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private AuthDTO authDTO;
    private UserDTO userDTO;

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
    }

    @Test
    void testAuthenticate_Success() throws ServiceException {
        // Arrange
        JwtResponse jwtResponse = new JwtResponse(
                "jwt-token-12345",
                "refresh-token-12345",
                "test@example.com",
                "John Doe",
                "ROLE_USER",
                "test@example.com",
                "http://example.com/profile.jpg",
                "user123");
        when(userService.authenticate(authDTO)).thenReturn(jwtResponse);

        // Act
        ResponseEntity<JwtResponse> response = authController.authenticate(authDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("jwt-token-12345", response.getBody().getJwtToken());
        assertEquals("John Doe", response.getBody().getName());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService, times(1)).authenticate(authDTO);
    }

    @Test
    void testAuthenticate_InvalidCredentials() {
        // Arrange
        when(userService.authenticate(authDTO))
                .thenThrow(new ServiceException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

        // Act & Assert
        assertThrows(ServiceException.class, () -> authController.authenticate(authDTO));
        verify(userService, times(1)).authenticate(authDTO);
    }

    @Test
    void testAuthenticate_UserDisabled() {
        // Arrange
        when(userService.authenticate(authDTO))
                .thenThrow(new ServiceException("USER_DISABLED", HttpStatus.FORBIDDEN));

        // Act & Assert
        assertThrows(ServiceException.class, () -> authController.authenticate(authDTO));
        verify(userService, times(1)).authenticate(authDTO);
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        UserDTO savedUser = new UserDTO();
        savedUser.setId("user123");
        savedUser.setEmail("test@example.com");
        savedUser.setName("John Doe");
        savedUser.setPhoneNumber("9999999999");
        savedUser.setPassword("********");

        when(userService.registerUser(any(UserDTO.class))).thenReturn(savedUser);

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = authController.createUser(userDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(HttpStatus.OK, response.getBody().getResponseCode());
        assertEquals("User created successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("********", response.getBody().getData().get(0).getPassword()); // Password masked
        verify(userService, times(1)).registerUser(any(UserDTO.class));
    }

    @Test
    void testCreateUser_UnexpectedError() {
        // Arrange
        when(userService.registerUser(any(UserDTO.class)))
                .thenThrow(new ServiceException("User creation failed", HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        assertThrows(ServiceException.class, () -> authController.createUser(userDTO));
        verify(userService, times(1)).registerUser(any(UserDTO.class));
    }
}
