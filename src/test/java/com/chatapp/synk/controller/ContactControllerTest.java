package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ContactDTO;
import com.chatapp.synk.dto.ContactUserDTO;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactService contactService;

    @InjectMocks
    private ContactController contactController;

    private ContactDTO mockContactDTO;
    private ContactUserDTO mockContactUserDTO;

    @BeforeEach
    void setUp() {
        mockContactDTO = new ContactDTO();
        mockContactDTO.setId("1");
        mockContactDTO.setUserId("user1");
        mockContactDTO.setContactUserId("user2");
        mockContactDTO.setContactStatus(ContactStatus.ADDED);
        mockContactDTO.setEmail("user2@example.com");

        mockContactUserDTO = new ContactUserDTO();
        mockContactUserDTO.setContactId("1");
        mockContactUserDTO.setName("Alice Johnson");
        mockContactUserDTO.setEmail("alice@example.com");
    }

    @Test
    void testGetContacts_WhenContactsExist() {
        // Arrange
        String userId = "user1";
        when(contactService.getContacts(userId)).thenReturn(List.of(mockContactUserDTO));

        // Act
        ResponseEntity<SuccessResponse<ContactUserDTO>> response = contactController.getContacts(userId);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(HttpStatus.OK, response.getBody().getResponseCode());
        assertEquals("Contacts fetched successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Alice Johnson", response.getBody().getData().get(0).getName());
        verify(contactService, times(1)).getContacts(userId);
    }

    @Test
    void testGetContacts_WhenNoContactsFound() {
        // Arrange
        String userId = "user1";
        when(contactService.getContacts(userId)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<SuccessResponse<ContactUserDTO>> response = contactController.getContacts(userId);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(HttpStatus.NOT_FOUND, response.getBody().getResponseCode());
        assertEquals("No contacts found", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(contactService, times(1)).getContacts(userId);
    }

    @Test
    void testAddContact_WhenContactStatusIsAdded() {
        // Arrange
        mockContactDTO.setContactStatus(ContactStatus.ADDED);
        when(contactService.addContact(any(ContactDTO.class))).thenReturn(mockContactDTO);

        // Act
        ResponseEntity<SuccessResponse<ContactDTO>> response = contactController.addContact(mockContactDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(HttpStatus.OK, response.getBody().getResponseCode());
        assertEquals("Contact created successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(ContactStatus.ADDED, response.getBody().getData().get(0).getContactStatus());
        verify(contactService, times(1)).addContact(any(ContactDTO.class));
    }

    @Test
    void testAddContact_WhenContactStatusIsInvited() {
        // Arrange
        mockContactDTO.setContactStatus(ContactStatus.INVITED);
        when(contactService.addContact(any(ContactDTO.class))).thenReturn(mockContactDTO);

        // Act
        ResponseEntity<SuccessResponse<ContactDTO>> response = contactController.addContact(mockContactDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(HttpStatus.OK, response.getBody().getResponseCode());
        assertEquals("Invitation sent successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(ContactStatus.INVITED, response.getBody().getData().get(0).getContactStatus());
        verify(contactService, times(1)).addContact(any(ContactDTO.class));
    }

    @Test
    void testDeleteContact_Success() {
        // Arrange
        String contactId = "1";
        doNothing().when(contactService).deleteContact(contactId);

        // Act
        ResponseEntity<SuccessResponse<String>> response = contactController.deleteContact(contactId);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(HttpStatus.OK, response.getBody().getResponseCode());
        assertEquals("Contact deleted successfully", response.getBody().getMessage());
        verify(contactService, times(1)).deleteContact(contactId);
    }
}
