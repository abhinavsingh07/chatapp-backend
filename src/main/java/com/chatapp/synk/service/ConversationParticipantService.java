package com.chatapp.synk.service;

import com.chatapp.synk.dto.ConversationParticipantDTO;

import java.util.List;

public interface ConversationParticipantService {
    ConversationParticipantDTO addParticipant(ConversationParticipantDTO dto);

    ConversationParticipantDTO getParticipantById(String id);

    List<ConversationParticipantDTO> getParticipantsByConversationId(String conversationId);

    void deleteByConversationid(String id);
}
