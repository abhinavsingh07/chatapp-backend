package com.chatapp.synk.entity;

import com.chatapp.synk.enums.UserStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", schema = "chatapp")
public class User {

    @Id
    @Column(name = "id", nullable = false,length = 100)
    private String id;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "password", length = 200)
    private String password;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "profile_picture_url", length = 255)
    private String profilePictureUrl;

    @Column(name = "about", columnDefinition = "TEXT")
    private String about;

    @Column(name = "created_at", nullable = false, updatable = false)
    //LocalDateTime is DATETIME data type in db
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    //LocalDateTime is DATETIME data type in db
    private LocalDateTime updatedAt;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    // Constructors
    public User() {
    }

    public User(String id, String phoneNumber,String email, String password, String name, String profilePictureUrl,
                String about, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password=password;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
        this.about = about;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
