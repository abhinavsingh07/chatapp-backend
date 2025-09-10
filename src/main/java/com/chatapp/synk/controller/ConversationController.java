package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ChatListDTO;
import com.chatapp.synk.dto.ConversationDTO;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ConversationLastMessageService;
import com.chatapp.synk.service.ConversationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);
    private final ConversationService conversationService;

    private final ConversationLastMessageService conversationLastMessageService;

    public ConversationController(ConversationService conversationService, ConversationLastMessageService conversationLastMessageService) {
        this.conversationService = conversationService;
        this.conversationLastMessageService = conversationLastMessageService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<ConversationDTO>> create(@Valid @RequestBody ConversationDTO dto) {
        ConversationDTO created = conversationService.createConversation(dto);
        logger.info("Conversation created successfully with ID: {}", created.getId());
        return ResponseEntity.ok(new SuccessResponse<>("201", "Conversation created", List.of(created)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<ConversationDTO>> getById(@PathVariable String id) {
        logger.debug("Fetching conversation with ID: {}", id);
        ConversationDTO convo = conversationService.getConversationById(id);

        if (convo != null) {
            logger.info("Conversation found with ID: {}", id);
            return ResponseEntity.ok(new SuccessResponse<>("200", "Conversation found", List.of(convo)));
        } else {
            logger.warn("Conversation not found with ID: {}", id);
            return ResponseEntity.ok(new SuccessResponse<>("404", "Conversation not found", Collections.emptyList()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<SuccessResponse<ConversationDTO>> getAllConversations() {
        logger.debug("Fetching all conversations");
        List<ConversationDTO> conversations = conversationService.findAll();

        if (conversations.isEmpty()) {
            logger.warn("No conversations found");
        } else {
            logger.info("Retrieved {} conversations", conversations.size());
        }

        String msg = conversations.isEmpty() ? "No conversations available" : "Conversations retrieved";
        String code = conversations.isEmpty() ? "404" : "200";

        return ResponseEntity.ok(new SuccessResponse<>(code, msg, conversations));
    }

    @PostMapping("/get-or-create/{fromUserId}/{toUserId}")
    public ResponseEntity<SuccessResponse<String>> getOrCreateConversation(@PathVariable String fromUserId, @PathVariable String toUserId) {
        logger.info("Request to get or create conversation between {} and {}", fromUserId, toUserId);

        String conversationId = conversationService.getOrCreateConversation(fromUserId, toUserId);

        if (conversationId == null) {
            logger.error("Failed to create or fetch conversation between {} and {}", fromUserId, toUserId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new SuccessResponse<>("500", "Failed to create or fetch conversation", null));
        }

        logger.info("Conversation {} found/created successfully between {} and {}", conversationId, fromUserId, toUserId);
        return ResponseEntity.ok(new SuccessResponse<>("200", "Conversation found/created successfully", List.of(conversationId)));
    }

    @GetMapping("/{userId}/chat-list")
    public ResponseEntity<SuccessResponse<ChatListDTO>> getUserChatList(@PathVariable String userId) {
        logger.debug("Fetching chat list for userId={}", userId);

        List<ChatListDTO> chatList = conversationLastMessageService.findUserConversations(userId);

        if (chatList.isEmpty()) {
            logger.warn("No conversations found for userId={}", userId);
        } else {
            logger.info("Retrieved {} conversations for userId={}", chatList.size(), userId);
        }

        String msg = chatList.isEmpty() ? "No conversations available" : "Conversations retrieved successfully";
        String code = chatList.isEmpty() ? "404" : "200";

        return ResponseEntity.ok(new SuccessResponse<>(code, msg, chatList));
    }

}
