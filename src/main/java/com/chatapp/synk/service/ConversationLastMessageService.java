package com.chatapp.synk.service;

import com.chatapp.synk.dto.ConversationLastMsgDTO;

import java.util.List;

public interface ConversationLastMessageService {
    void upsertLastMessage(String conversationId, String messageId, String senderId, String content);

    List<ConversationLastMsgDTO> findUserConversations(String loggedInUserId);
}
