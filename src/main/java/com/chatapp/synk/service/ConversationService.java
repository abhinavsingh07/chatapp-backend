package com.chatapp.synk.service;

import com.chatapp.synk.dto.ConversationDTO;

import java.util.List;

public interface ConversationService {
    ConversationDTO createConversation(ConversationDTO dto);

    ConversationDTO getConversationById(String id);

    List<ConversationDTO> findAll();

    String getOrCreateConversation(String userId, String contactUserId);
}
