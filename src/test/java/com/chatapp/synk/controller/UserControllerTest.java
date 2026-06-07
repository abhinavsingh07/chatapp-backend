package com.chatapp.synk.controller;

import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.dto.UserStatusDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserDTO mockUser;
    private UserStatusDTO mockUserStatus;

    @BeforeEach
    void setUp() {
        mockUser = new UserDTO();
        mockUser.setId("1");
        mockUser.setName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setPhoneNumber("9999999999");

        mockUserStatus = new UserStatusDTO("1", true, "2025-05-24T10:30:00");
    }

    @Test
    void testGetAllUsers_WhenUsersExist() {
        // Arrange
        when(userService.getAllUsers()).thenReturn(List.of(mockUser));

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = userController.getAllUsers();

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Users fetched successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("John Doe", response.getBody().getData().get(0).getName());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void testGetAllUsers_WhenNoUsersExist() {
        // Arrange
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = userController.getAllUsers();

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("404", response.getBody().getResponseCode());
        assertEquals("No users found", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void testGetUserById_WhenUserExists() {
        // Arrange
        when(userService.getUserById("1")).thenReturn(mockUser);

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = userController.getUserById("1");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("User fetched", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("John Doe", response.getBody().getData().get(0).getName());
        assertEquals("john@example.com", response.getBody().getData().get(0).getEmail());
        verify(userService, times(1)).getUserById("1");
    }

    @Test
    void testGetUserById_WhenUserNotFound() {
        // Arrange
        when(userService.getUserById("99")).thenReturn(null);

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = userController.getUserById("99");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("404", response.getBody().getResponseCode());
        assertEquals("User not found", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(userService, times(1)).getUserById("99");
    }

    @Test
    void testUpdateUser_Success() {
        // Arrange
        UserDTO updatedUser = new UserDTO();
        updatedUser.setId("1");
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setPhoneNumber("9999999999");

        when(userService.updateUser("1", mockUser)).thenReturn(updatedUser);

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = userController.updateUser("1", mockUser);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("User updated successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Updated Name", response.getBody().getData().get(0).getName());
        assertEquals("updated@example.com", response.getBody().getData().get(0).getEmail());
        verify(userService, times(1)).updateUser("1", mockUser);
    }

    @Test
    void testUpdateUser_WithPasswordChange_Success() {
        // Arrange
        UserDTO passwordChangeRequest = new UserDTO();
        passwordChangeRequest.setOldPassword("OldPassword1");
        passwordChangeRequest.setNewPassword("NewPassword1");
        passwordChangeRequest.setConfirmPassword("NewPassword1");

        UserDTO updatedUser = new UserDTO();
        updatedUser.setId("1");
        updatedUser.setName("John Doe");
        updatedUser.setEmail("john@example.com");
        updatedUser.setPhoneNumber("9999999999");
        updatedUser.setPassword("********");

        when(userService.updateUser("1", passwordChangeRequest)).thenReturn(updatedUser);

        // Act
        ResponseEntity<SuccessResponse<UserDTO>> response = userController.updateUser("1", passwordChangeRequest);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("User updated successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("********", response.getBody().getData().get(0).getPassword());
        verify(userService, times(1)).updateUser("1", passwordChangeRequest);
    }

    @Test
    void testDeleteUser_Success() {
        // Arrange
        doNothing().when(userService).deleteUser("1");

        // Act
        ResponseEntity<SuccessResponse<Void>> response = userController.deleteUser("1");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("User deleted successfully", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(userService, times(1)).deleteUser("1");
    }

    @Test
    void testGetLastActiveUserStatus_WhenStatusExists() {
        // Arrange
        when(userService.getLastActiveUserStatus("1")).thenReturn(List.of(mockUserStatus));

        // Act
        ResponseEntity<SuccessResponse<UserStatusDTO>> response = userController.getLastActiveUserStatus("1");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("User statuses fetched", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        verify(userService, times(1)).getLastActiveUserStatus("1");
    }

    @Test
    void testGetLastActiveUserStatus_WhenUserIdIsNull() {
        // Act
        ResponseEntity<SuccessResponse<UserStatusDTO>> response = userController.getLastActiveUserStatus(null);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("400", response.getBody().getResponseCode());
        assertEquals("No user ID provided", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
    }

    @Test
    void testGetLastActiveUserStatus_WhenUserIdIsEmpty() {
        // Act
        ResponseEntity<SuccessResponse<UserStatusDTO>> response = userController.getLastActiveUserStatus("");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("400", response.getBody().getResponseCode());
        assertEquals("No user ID provided", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
    }

    @Test
    void testGetLastActiveUserStatus_WhenStatusNotFound() {
        // Arrange
        when(userService.getLastActiveUserStatus("99")).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<SuccessResponse<UserStatusDTO>> response = userController.getLastActiveUserStatus("99");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("404", response.getBody().getResponseCode());
        assertEquals("No status found", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(userService, times(1)).getLastActiveUserStatus("99");
    }
}
