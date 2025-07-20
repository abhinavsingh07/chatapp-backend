package com.chatapp.synk.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_participants", schema = "chatapp")
public class ConversationParticipant {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ConversationParticipant() {
    }

    public ConversationParticipant(String id, String conversationId, String userId, LocalDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    // Getters and Setters


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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
