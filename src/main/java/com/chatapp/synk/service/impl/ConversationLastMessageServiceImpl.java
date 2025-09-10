package com.chatapp.synk.service.impl;

import com.chatapp.synk.dto.ChatListDTO;
import com.chatapp.synk.repository.ConversationLastMessageRepository;
import com.chatapp.synk.security_validator.InputSecurityUtils;
import com.chatapp.synk.service.ConversationLastMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationLastMessageServiceImpl implements ConversationLastMessageService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationLastMessageServiceImpl.class);

    private final ConversationLastMessageRepository conversationLastMessageRepository;

    public ConversationLastMessageServiceImpl(ConversationLastMessageRepository conversationLastMessageRepository) {
        this.conversationLastMessageRepository = conversationLastMessageRepository;
    }

    @Override
    public void upsertLastMessage(String conversationId, String messageId, String senderId, String content) {
        String conversationValidId = InputSecurityUtils.secureId(conversationId);
        String messageValidId = InputSecurityUtils.secureId(messageId);
        String senderValidId = InputSecurityUtils.secureId(senderId);
        String safeContent = InputSecurityUtils.secureName(content); // Reusing name sanitizer for message content
        logger.info("Upserting last message for conversationId={}, messageId={}", conversationValidId, messageValidId);
        conversationLastMessageRepository.upsertLastMessage(conversationValidId, messageValidId, senderValidId, safeContent);
        logger.info("Successfully upserted last message for conversationId={}", conversationValidId);
    }

    @Override
    public List<ChatListDTO> findUserConversations(String loggedInUserId) {
        String validUserId = InputSecurityUtils.secureId(loggedInUserId);
        logger.info("Fetching chat list for userId={}", validUserId);
        List<ChatListDTO> chatList = conversationLastMessageRepository.findUserConversations(validUserId);
        logger.info("Fetched {} conversations for userId={}", chatList.size(), validUserId);
        return chatList;
    }
}
