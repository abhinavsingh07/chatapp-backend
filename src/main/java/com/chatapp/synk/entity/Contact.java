package com.chatapp.synk.entity;

import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.EmailStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contacts", schema = "chatapp", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "contact_user_id"}))
public class Contact {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "contact_user_id", length = 100)
    private String contactUserId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "contact_status")
    @Enumerated(EnumType.STRING)
    private ContactStatus contactStatus;
    @Column(name = "email_status")
    @Enumerated(EnumType.STRING)
    private EmailStatus emailStatus;

    @Column(name = "email", length = 100)
    private String email;

    public Contact() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(String contactUserId) {
        this.contactUserId = contactUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ContactStatus getContactStatus() {
        return contactStatus;
    }

    public void setContactStatus(ContactStatus contactStatus) {
        this.contactStatus = contactStatus;
    }

    public EmailStatus getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(EmailStatus emailStatus) {
        this.emailStatus = emailStatus;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
