package com.chatapp.synk.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "conversation_last_message", schema = "chatapp")
public class ConversationLastMessage {

    @Id
    @Column(name = "conversation_id", nullable = false, length = 50)//this is unique and PK our on duplicate key update query works.
    private String conversationId;
    @Column(name = "message_id", nullable = false, length = 50)
    private String messageId;
    @Column(name = "sender_id", nullable = false, length = 50)
    private String senderId;
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;//Instant is UTC time client converts this time on their browser or mobile sdk and get its time according to it timezone
    @Column(name = "updated_at",nullable = false)
    //LocalDateTime is DATETIME data type in db
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        sentAt = Instant.now(); // Always UTC
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        sentAt = Instant.now(); // Always UTC
    }


    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
