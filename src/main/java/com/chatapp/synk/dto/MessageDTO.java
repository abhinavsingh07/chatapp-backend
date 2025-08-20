package com.chatapp.synk.dto;

import com.chatapp.synk.enums.MessageStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class MessageDTO {

    private String id;
    @NotBlank(message = "Conversation ID is required")
    private String conversationId;
    @NotBlank(message = "Sender ID is required")
    private String senderId;
    @NotBlank(message = "Receiver ID is required")
    private String receiverId;
    private String content;
    private String mediaId;
    private MessageStatus messageStatus;
    private LocalDateTime sentAt;

    public MessageDTO() {

    }

    // Getters & Setters
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

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}