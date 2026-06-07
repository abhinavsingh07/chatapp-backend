package com.chatapp.synk.controller;

import com.chatapp.synk.dto.MessageDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.MessageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<SuccessResponse<MessageDTO>> getMessagesByConversationId(@PathVariable String conversationId) {
        logger.debug("Fetching messages for conversation: {}", conversationId);
        List<MessageDTO> messages = messageService.getMessagesByConversationId(conversationId);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "Messages fetched successfully", messages));
    }

    @GetMapping("/unread/{conversationId}/{receiverId}")
    public ResponseEntity<SuccessResponse<MessageDTO>> getUnreadMessages(@PathVariable("conversationId") String conversationId, @PathVariable("receiverId") String receiverId) {

        logger.debug("Fetching unread messages for receiver: {} in conversation: {}", receiverId, conversationId);
        List<MessageDTO> messages = messageService.getUnreadMessagesForReceiver(conversationId, receiverId);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "Unread messages fetched", messages));
    }

    @PostMapping("/create")
    public ResponseEntity<SuccessResponse<MessageDTO>> saveMessage(@Valid @RequestBody MessageDTO messageDTO) {
        logger.info("New message send request for conversationId: {}", messageDTO.getConversationId());
        MessageDTO saved = messageService.saveMessage(messageDTO);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "Message sent successfully", List.of(saved)));
    }

    @PutMapping("/read/{id}")
    public ResponseEntity<SuccessResponse<Void>> markMessageAsRead(@PathVariable String id) {
        logger.debug("Marking message as read with ID: {}", id);
        messageService.markMessageAsRead(id);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "Message marked as read", Collections.emptyList()));
    }
}

