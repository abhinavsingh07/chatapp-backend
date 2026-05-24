package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ConversationDTO;
import com.chatapp.synk.dto.ConversationLastMsgDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ConversationLastMessageService;
import com.chatapp.synk.service.ConversationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private ConversationLastMessageService conversationLastMessageService;

    @InjectMocks
    private ConversationController conversationController;

    private ConversationDTO mockConversationDTO;

    @BeforeEach
    void setUp() {
        mockConversationDTO = new ConversationDTO();
        mockConversationDTO.setId("convo123");
        mockConversationDTO.setConversationType("ONE_TO_ONE");
    }

    @Test
    void testCreate_Success() {
        // Arrange
        when(conversationService.createConversation(any(ConversationDTO.class)))
            .thenReturn(mockConversationDTO);

        // Act
        ResponseEntity<SuccessResponse<ConversationDTO>> response =
            conversationController.create(mockConversationDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("201", response.getBody().getResponseCode());
        assertEquals("Conversation created", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("convo123", response.getBody().getData().get(0).getId());
        verify(conversationService, times(1)).createConversation(any(ConversationDTO.class));
    }

    @Test
    void testGetById_WhenConversationFound() {
        // Arrange
        when(conversationService.getConversationById("convo123"))
            .thenReturn(mockConversationDTO);

        // Act
        ResponseEntity<SuccessResponse<ConversationDTO>> response =
            conversationController.getById("convo123");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Conversation found", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("convo123", response.getBody().getData().get(0).getId());
        verify(conversationService, times(1)).getConversationById("convo123");
    }

    @Test
    void testGetById_WhenConversationNotFound() {
        // Arrange
        when(conversationService.getConversationById("nonexistent"))
            .thenReturn(null);

        // Act
        ResponseEntity<SuccessResponse<ConversationDTO>> response =
            conversationController.getById("nonexistent");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("404", response.getBody().getResponseCode());
        assertEquals("Conversation not found", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(conversationService, times(1)).getConversationById("nonexistent");
    }

    @Test
    void testGetAllConversations_WhenConversationsExist() {
        // Arrange
        when(conversationService.findAll()).thenReturn(List.of(mockConversationDTO));

        // Act
        ResponseEntity<SuccessResponse<ConversationDTO>> response =
            conversationController.getAllConversations();

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Conversations retrieved", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        verify(conversationService, times(1)).findAll();
    }

    @Test
    void testGetAllConversations_WhenNoConversationsFound() {
        // Arrange
        when(conversationService.findAll()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<SuccessResponse<ConversationDTO>> response =
            conversationController.getAllConversations();

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("404", response.getBody().getResponseCode());
        assertEquals("No conversations available", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(conversationService, times(1)).findAll();
    }

    @Test
    void testGetOrCreateConversation_Success() {
        // Arrange
        when(conversationService.getOrCreateConversation("user1", "user2"))
            .thenReturn("convo456");

        // Act
        ResponseEntity<SuccessResponse<String>> response =
            conversationController.getOrCreateConversation("user1", "user2");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Conversation found/created successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("convo456", response.getBody().getData().get(0));
        verify(conversationService, times(1)).getOrCreateConversation("user1", "user2");
    }

    @Test
    void testGetOrCreateConversation_Failure() {
        // Arrange
        when(conversationService.getOrCreateConversation("user1", "user2"))
            .thenReturn(null);

        // Act
        ResponseEntity<SuccessResponse<String>> response =
            conversationController.getOrCreateConversation("user1", "user2");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is5xxServerError());
        assertEquals("500", response.getBody().getResponseCode());
        assertEquals("Failed to create or fetch conversation", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(conversationService, times(1)).getOrCreateConversation("user1", "user2");
    }

    @Test
    void testGetUserConversationsLastMessage_WhenConversationsExist() {
        // Arrange
        ConversationLastMsgDTO mockLastMsgDTO = mock(ConversationLastMsgDTO.class);

        when(conversationLastMessageService.findUserConversations("user1"))
            .thenReturn(List.of(mockLastMsgDTO));

        // Act
        ResponseEntity<SuccessResponse<ConversationLastMsgDTO>> response =
            conversationController.getUserConversationsLastMessage("user1");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Conversations retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        verify(conversationLastMessageService, times(1)).findUserConversations("user1");
    }

    @Test
    void testGetUserConversationsLastMessage_WhenNoConversationsFound() {
        // Arrange
        when(conversationLastMessageService.findUserConversations("user1"))
            .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<SuccessResponse<ConversationLastMsgDTO>> response =
            conversationController.getUserConversationsLastMessage("user1");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("404", response.getBody().getResponseCode());
        assertEquals("No conversations available", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(conversationLastMessageService, times(1)).findUserConversations("user1");
    }
}
