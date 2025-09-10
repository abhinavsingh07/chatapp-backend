package com.chatapp.synk.dto;

import java.time.Instant;

public class ChatListDTO {
    private String lastMessageId;
    private String conversationId;
    private String content;
    private Instant sentAt;
    private String senderId;

    private String participantId;
    private String participantName;
    private String participantProfilePic;

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public void setParticipantProfilePic(String participantProfilePic) {
        this.participantProfilePic = participantProfilePic;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public String getParticipantProfilePic() {
        return participantProfilePic;
    }
}
