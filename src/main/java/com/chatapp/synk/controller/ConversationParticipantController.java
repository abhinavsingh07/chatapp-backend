package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ConversationParticipantDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ConversationParticipantService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/participants")
public class ConversationParticipantController {
    private static final Logger logger = LoggerFactory.getLogger(ConversationParticipantController.class);

    @Autowired
    private ConversationParticipantService participantService;

    @PostMapping
    public ResponseEntity<SuccessResponse<ConversationParticipantDTO>> add(@Valid @RequestBody ConversationParticipantDTO dto) {
        logger.info("Request to add participant to conversation {}", dto.getConversationId());
        ConversationParticipantDTO added = participantService.addParticipant(dto);
        return ResponseEntity.ok(new SuccessResponse<>("201", "Participant added", List.of(added)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<ConversationParticipantDTO>> getById(@PathVariable(required = true) String id) {
        logger.info("Fetching participant with ID: {}", id);
        ConversationParticipantDTO participant = participantService.getParticipantById(id);

        if (participant != null) {
            return ResponseEntity.ok(new SuccessResponse<>("200", "Participant found", List.of(participant)));
        } else {
            return ResponseEntity.ok(new SuccessResponse<>("404", "Participant not found", Collections.emptyList()));
        }
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<SuccessResponse<ConversationParticipantDTO>> getByConversation(@PathVariable(required = true) String conversationId) {
        logger.info("Fetching all participants for conversation ID: {}", conversationId);
        List<ConversationParticipantDTO> participants = participantService.getParticipantsByConversationId(conversationId);

        String code = participants.isEmpty() ? "404" : "200";
        String msg = participants.isEmpty() ? "No participants found" : "Participants retrieved";
        return ResponseEntity.ok(new SuccessResponse<>(code, msg, participants));
    }

    @DeleteMapping("/conversation/{id}")
    public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable(required = true) String id) {
        logger.info("Deleting participant with ID: {}", id);
        participantService.deleteByConversationid(id);
        return ResponseEntity.ok(new SuccessResponse<>("200", "Participant removed", Collections.emptyList()));
    }

}
