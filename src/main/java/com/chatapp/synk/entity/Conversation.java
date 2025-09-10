package com.chatapp.synk.entity;

import com.chatapp.synk.enums.ConversationType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", schema = "chatapp")
public class Conversation {
    public static final String ALIAS_CONVERSATION = "CONV";
    @Id
    @Column(name = "id", nullable = false,length = 50)
    private String id;

    @Column(name = "conversation_type",length = 10)
    private String conversationType = ConversationType.ONE_TO_ONE.toString();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Conversation(String id, String conversationType) {
        this.id = id;
        this.conversationType = conversationType;
    }

    public Conversation() {
        // Default constructor
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationType() {
        return conversationType;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
