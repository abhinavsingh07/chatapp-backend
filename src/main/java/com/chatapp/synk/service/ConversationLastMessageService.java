package com.chatapp.synk.service;

import com.chatapp.synk.dto.ChatListDTO;

import java.util.List;

public interface ConversationLastMessageService {
    void upsertLastMessage(String conversationId, String messageId, String senderId, String content);

    List<ChatListDTO> findUserConversations(String loggedInUserId);
}
