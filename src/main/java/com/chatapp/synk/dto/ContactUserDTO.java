package com.chatapp.synk.dto;

import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.EmailStatus;
import com.chatapp.synk.enums.UserStatus;

public class ContactUserDTO {
    private String contactId;
    private String userId;
    private ContactStatus contactStatus;
    private EmailStatus emailStatus;
    private String contactUserId;
    private String contactEmail;

    //user table fields
    private String name;
    private String phoneNumber;
    private String email;
    private String profilePictureUrl;
    private UserStatus status;
    private Boolean isMutual;


    //using JPQL query
    public ContactUserDTO(String contactId, ContactStatus contactStatus, EmailStatus emailStatus, String contactUserId,String contactEmail, String name, String phoneNumber, String email, String profilePictureUrl, UserStatus status,Boolean isMutual,String userId) {
        this.contactId = contactId;
        this.contactStatus = contactStatus;
        this.emailStatus = emailStatus;
        this.contactUserId = contactUserId;
        this.contactEmail=contactEmail;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.status = status;
        this.isMutual=isMutual;
        this.userId=userId;
    }

    public ContactUserDTO(String contactId, ContactStatus contactStatus, EmailStatus emailStatus, String contactUserId, String contactEmail, String name, String phoneNumber, String email, String profilePictureUrl, UserStatus status,String userId) {
        this.contactId = contactId;
        this.contactStatus = contactStatus;
        this.emailStatus = emailStatus;
        this.contactUserId = contactUserId;
        this.contactEmail = contactEmail;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.status = status;
        this.isMutual=false;
        this.userId=userId;
    }

    public ContactUserDTO() {
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
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

    public String getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(String contactUserId) {
        this.contactUserId = contactUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getMutual() {
        return isMutual;
    }

    public void setMutual(Boolean mutual) {
        isMutual = mutual;
    }
}
