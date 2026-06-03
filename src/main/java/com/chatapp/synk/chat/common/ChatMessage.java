package com.chatapp.synk.chat.common;

import com.chatapp.synk.enums.ChatWebSocketStatus;

public class ChatMessage {

    private ChatWebSocketStatus wsStatus;
    private String conversationId;
    private String fromUserId;
    private String toUserId;
    private String body;
    private String sentAt;
    private String lastActiveTimeStamp;// only for presence it has utc time
    private String fromUserName;

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

    public ChatWebSocketStatus getWsStatus() {
        return wsStatus;
    }

    public void setWsStatus(ChatWebSocketStatus wsStatus) {
        this.wsStatus = wsStatus;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public String getLastActiveTimeStamp() {
        return lastActiveTimeStamp;
    }

    public void setLastActiveTimeStamp(String lastActiveTimeStamp) {
        this.lastActiveTimeStamp = lastActiveTimeStamp;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }
}