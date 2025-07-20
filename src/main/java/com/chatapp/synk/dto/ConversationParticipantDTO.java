package com.chatapp.synk.dto;

import jakarta.validation.constraints.NotBlank;

public class ConversationParticipantDTO {

    private String id;

    @NotBlank(message = "Conversation ID is required")
    private String conversationId;

    @NotBlank(message = "User ID is required")
    private String userId;

    public ConversationParticipantDTO() {
    }

    public ConversationParticipantDTO(String id, String conversationId, String userId) {
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
