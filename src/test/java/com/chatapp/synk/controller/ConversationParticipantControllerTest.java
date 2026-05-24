package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ConversationParticipantDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ConversationParticipantService;
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
class ConversationParticipantControllerTest {

    @Mock
    private ConversationParticipantService participantService;

    @InjectMocks
    private ConversationParticipantController participantController;

    private ConversationParticipantDTO mockParticipantDTO;

    @BeforeEach
    void setUp() {
        mockParticipantDTO = new ConversationParticipantDTO();
        mockParticipantDTO.setId("participant1");
        mockParticipantDTO.setConversationId("convo123");
        mockParticipantDTO.setUserId("user1");
    }

    @Test
    void testAdd_Success() {
        // Arrange
        when(participantService.addParticipant(any(ConversationParticipantDTO.class)))
            .thenReturn(mockParticipantDTO);

        // Act
        ResponseEntity<SuccessResponse<ConversationParticipantDTO>> response =
            participantController.add(mockParticipantDTO);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("201", response.getBody().getResponseCode());
        assertEquals("Participant added", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("participant1", response.getBody().getData().get(0).getId());
        verify(participantService, times(1)).addParticipant(any(ConversationParticipantDTO.class));
    }

    @Test
    void testGetById_WhenParticipantFound() {
        // Arrange
        when(participantService.getParticipantById("participant1"))
            .thenReturn(mockParticipantDTO);

        // Act
        ResponseEntity<SuccessResponse<ConversationParticipantDTO>> response =
            participantController.getById("participant1");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Participant found", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("participant1", response.getBody().getData().get(0).getId());
        verify(participantService, times(1)).getParticipantById("participant1");
    }

    @Test
    void testGetById_WhenParticipantNotFound() {
        // Arrange
        when(participantService.getParticipantById("nonexistent"))
            .thenReturn(null);

        // Act
        ResponseEntity<SuccessResponse<ConversationParticipantDTO>> response =
            participantController.getById("nonexistent");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("404", response.getBody().getResponseCode());
        assertEquals("Participant not found", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(participantService, times(1)).getParticipantById("nonexistent");
    }

    @Test
    void testGetByConversation_WhenParticipantsFound() {
        // Arrange
        when(participantService.getParticipantsByConversationId("convo123"))
            .thenReturn(List.of(mockParticipantDTO));

        // Act
        ResponseEntity<SuccessResponse<ConversationParticipantDTO>> response =
            participantController.getByConversation("convo123");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Participants retrieved", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        verify(participantService, times(1)).getParticipantsByConversationId("convo123");
    }

    @Test
    void testGetByConversation_WhenNoParticipantsFound() {
        // Arrange
        when(participantService.getParticipantsByConversationId("convo999"))
            .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<SuccessResponse<ConversationParticipantDTO>> response =
            participantController.getByConversation("convo999");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("404", response.getBody().getResponseCode());
        assertEquals("No participants found", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(participantService, times(1)).getParticipantsByConversationId("convo999");
    }

    @Test
    void testDelete_Success() {
        // Arrange
        doNothing().when(participantService).deleteByConversationid("participant1");

        // Act
        ResponseEntity<SuccessResponse<Void>> response =
            participantController.delete("participant1");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("200", response.getBody().getResponseCode());
        assertEquals("Participant removed", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(participantService, times(1)).deleteByConversationid("participant1");
    }
}
