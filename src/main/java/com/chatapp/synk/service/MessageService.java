package com.chatapp.synk.service;

import com.chatapp.synk.dto.MessageDTO;

import java.util.List;

public interface MessageService {
    List<MessageDTO> getMessagesByConversationId(String conversationId);

    List<MessageDTO> getUnreadMessagesForReceiver(String conversationId, String receiverId);

    MessageDTO saveMessage(MessageDTO messageDTO);

    void markMessageAsRead(String messageId);
}