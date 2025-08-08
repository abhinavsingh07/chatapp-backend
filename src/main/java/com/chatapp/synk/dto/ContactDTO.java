package com.chatapp.synk.dto;

import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.EmailStatus;
import jakarta.validation.constraints.NotBlank;

public class  ContactDTO {

    private String id;
    @NotBlank(message = "User Id is required")
    private String userId;
    @NotBlank(message = "Email is required")
    private String email;
    private String contactUserId;
    private ContactStatus contactStatus;
    private EmailStatus emailStatus;

    private UserDTO user;

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

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}
