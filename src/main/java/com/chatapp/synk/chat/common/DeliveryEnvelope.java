package com.chatapp.synk.chat.common;

public class DeliveryEnvelope {
    private ChatMessage message;
    private String targetUserId;     // who should receive it
    private String targetServerId;   // server hosting the user
    private String targetSessionId;  // ws session on that server (if known)

    public DeliveryEnvelope() {
    }

    public DeliveryEnvelope(ChatMessage message, String targetUserId, String targetServerId, String targetSessionId) {
        this.message = message;
        this.targetUserId = targetUserId;
        this.targetServerId = targetServerId;
        this.targetSessionId = targetSessionId;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetServerId() {
        return targetServerId;
    }

    public void setTargetServerId(String targetServerId) {
        this.targetServerId = targetServerId;
    }

    public String getTargetSessionId() {
        return targetSessionId;
    }

    public void setTargetSessionId(String targetSessionId) {
        this.targetSessionId = targetSessionId;
    }

    @Override
    public String toString() {
        return "DeliveryEnvelope{" +
                "message=" + message +
                ", targetUserId='" + targetUserId + '\'' +
                ", targetServerId='" + targetServerId + '\'' +
                ", targetSessionId='" + targetSessionId + '\'' +
                '}';
    }
}
