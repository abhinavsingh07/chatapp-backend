package com.chatapp.synk.entity;

import com.chatapp.synk.enums.MessageStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", schema = "chatapp")
public class Message {

    public static final String ALIAS_MESSAGE = "MESG";

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "conversation_id", nullable = false, length = 50)
    private String conversationId;

    @Column(name = "sender_id", nullable = false, length = 50)
    private String senderId;

    @Column(name = "receiver_id", nullable = false, length = 50)
    private String receiverId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_id", length = 50)
    private String mediaId;

    @Column(name = "message_status")
    @Enumerated(EnumType.STRING)
    private MessageStatus messageStatus = MessageStatus.SENT;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
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
