package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ConversationParticipantDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ConversationParticipantService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/participants")
public class ConversationParticipantController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationParticipantController.class);
    private final ConversationParticipantService participantService;

    public ConversationParticipantController(ConversationParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<ConversationParticipantDTO>> add(@Valid @RequestBody ConversationParticipantDTO dto) {
        logger.info("Adding participant to conversation {}", dto.getConversationId());
        ConversationParticipantDTO added = participantService.addParticipant(dto);
        return ResponseEntity.ok(new SuccessResponse<>("201", "Participant added", List.of(added)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<ConversationParticipantDTO>> getById(@PathVariable String id) {
        ConversationParticipantDTO participant = participantService.getParticipantById(id);
        if (participant != null) {
            return ResponseEntity.ok(new SuccessResponse<>("200", "Participant found", List.of(participant)));
        } else {
            logger.debug("Participant not found with ID: {}", id);
            return ResponseEntity.ok(new SuccessResponse<>("404", "Participant not found", Collections.emptyList()));
        }
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<SuccessResponse<ConversationParticipantDTO>> getByConversation(@PathVariable String conversationId) {
        List<ConversationParticipantDTO> participants = participantService.getParticipantsByConversationId(conversationId);
        String code = participants.isEmpty() ? "404" : "200";
        String msg = participants.isEmpty() ? "No participants found" : "Participants retrieved";
        if (participants.isEmpty()) {
            logger.debug("No participants found for conversation {}", conversationId);
        }
        return ResponseEntity.ok(new SuccessResponse<>(code, msg, participants));
    }

    @DeleteMapping("/conversation/{id}")
    public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable String id) {
        logger.info("Removing participant with ID: {}", id);
        participantService.deleteByConversationid(id);
        return ResponseEntity.ok(new SuccessResponse<>("200", "Participant removed", Collections.emptyList()));
    }
}

