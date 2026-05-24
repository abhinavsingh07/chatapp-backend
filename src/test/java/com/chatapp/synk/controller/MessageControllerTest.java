package com.chatapp.synk.controller;

import com.chatapp.synk.dto.MessageDTO;
import com.chatapp.synk.enums.MessageStatus;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.MessageService;
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
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageController messageController;

    private MessageDTO mockMessageDTO;

    @BeforeEach
    void setUp() {
        mockMessageDTO = new MessageDTO();
        mockMessageDTO.setId("msg1");
        mockMessageDTO.setConversationId("convo123");
        mockMessageDTO.setSenderId("user1");
        mockMessageDTO.setReceiverId("user2");
        mockMessageDTO.setContent("Hello! How are you?");
        mockMessageDTO.setMessageStatus(MessageStatus.SENT);
    }

    @Test
    void testGetMessagesByConversationId_WhenMessagesExist() {
        // Arrange
        when(messageService.getMessagesByConversationId("convo123"))
            .thenReturn(List.of(mockMessageDTO));

        // Act
        ResponseEntity<SuccessResponse<MessageDTO>> response =
            messageController.getMessagesByConversationId("convo123");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Messages fetched successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Hello! How are you?", response.getBody().getData().get(0).getContent());
        verify(messageService, times(1)).getMessagesByConversationId("convo123");
    }

    @Test
    void testGetMessagesByConversationId_WhenNoMessagesFound() {
        // Arrange
        when(messageService.getMessagesByConversationId("convo999"))
            .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<SuccessResponse<MessageDTO>> response =
            messageController.getMessagesByConversationId("convo999");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Messages fetched successfully", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(messageService, times(1)).getMessagesByConversationId("convo999");
    }

    @Test
    void testGetUnreadMessages_WhenUnreadMessagesExist() {
        // Arrange
        MessageDTO unreadMessage = new MessageDTO();
        unreadMessage.setId("msg2");
        unreadMessage.setConversationId("convo123");
        unreadMessage.setSenderId("user1");
        unreadMessage.setReceiverId("user2");
        unreadMessage.setContent("Unread message");
        unreadMessage.setMessageStatus(MessageStatus.DELIVERED);

        when(messageService.getUnreadMessagesForReceiver("convo123", "user2"))
            .thenReturn(List.of(unreadMessage));

        // Act
        ResponseEntity<SuccessResponse<MessageDTO>> response =
            messageController.getUnreadMessages("convo123", "user2");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Unread messages fetched", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Unread message", response.getBody().getData().get(0).getContent());
        verify(messageService, times(1)).getUnreadMessagesForReceiver("convo123", "user2");
    }

    @Test
    void testGetUnreadMessages_WhenNoUnreadMessagesFound() {
        // Arrange
        when(messageService.getUnreadMessagesForReceiver("convo123", "user2"))
            .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<SuccessResponse<MessageDTO>> response =
            messageController.getUnreadMessages("convo123", "user2");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Unread messages fetched", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(messageService, times(1)).getUnreadMessagesForReceiver("convo123", "user2");
    }

    @Test
    void testSaveMessage_Success() {
        // Arrange
        MessageDTO messageToSave = new MessageDTO();
        messageToSave.setConversationId("convo123");
        messageToSave.setSenderId("user1");
        messageToSave.setReceiverId("user2");
        messageToSave.setContent("New message");

        MessageDTO savedMessage = new MessageDTO();
        savedMessage.setId("msg3");
        savedMessage.setConversationId("convo123");
        savedMessage.setSenderId("user1");
        savedMessage.setReceiverId("user2");
        savedMessage.setContent("New message");
        savedMessage.setMessageStatus(MessageStatus.SENT);

        when(messageService.saveMessage(any(MessageDTO.class))).thenReturn(savedMessage);

        // Act
        ResponseEntity<SuccessResponse<MessageDTO>> response =
            messageController.saveMessage(messageToSave);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Message sent successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("msg3", response.getBody().getData().get(0).getId());
        verify(messageService, times(1)).saveMessage(any(MessageDTO.class));
    }

    @Test
    void testMarkMessageAsRead_Success() {
        // Arrange
        doNothing().when(messageService).markMessageAsRead("msg1");

        // Act
        ResponseEntity<SuccessResponse<Void>> response =
            messageController.markMessageAsRead("msg1");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Message marked as read", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(messageService, times(1)).markMessageAsRead("msg1");
    }
}
