package com.chatapp.synk.dto;

import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.EmailStatus;
import jakarta.validation.constraints.NotBlank;

public class  ContactDTO {

    private String id;
    @NotBlank(message = "UserId is required")
    private String userId;
    private String contactUserId;
    private ContactStatus status;
    private EmailStatus emailStatus;

    public ContactDTO(){
    }

    public ContactDTO(String id, String userId, String contactUserId) {
        this.id = id;
        this.userId = userId;
        this.contactUserId = contactUserId;
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

    public ContactStatus getStatus() {
        return status;
    }

    public void setStatus(ContactStatus status) {
        this.status = status;
    }

    public EmailStatus getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(EmailStatus emailStatus) {
        this.emailStatus = emailStatus;
    }
}
