package com.chatapp.synk.dto;

public interface ConversationLastMsgDTO {
    String getLastMessageId();

    String getConversationId();

    String getContent();

    String getSentAt();

    String getSenderId();

    String getConversationType();

    String getParticipantId();

    String getParticipantName();

    String getParticipantProfilePic();
}