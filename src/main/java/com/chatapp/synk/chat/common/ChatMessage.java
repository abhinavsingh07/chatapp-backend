package com.chatapp.synk.chat.common;

public class ChatMessage {
    private String conversationId;
    private String fromUserId;
    private String toUserId;
    private String body;

    public ChatMessage() {
    }

    public ChatMessage(String conversationId, String fromUserId, String toUserId, String body) {
        this.conversationId = conversationId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.body = body;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}