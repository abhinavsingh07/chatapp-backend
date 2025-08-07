package com.chatapp.synk.dto;


import com.chatapp.synk.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;

public class UserDTO {

    private String id;//userid
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Password is required")
    private String password;
    @NotBlank(message = "Name is required")
    private String name;
    private String profilePictureUrl;
    private String about;
    private String userRoles;
    private UserStatus status;

    public UserDTO() {
    }

    //used in test class
    public UserDTO(String id, String phoneNumber, String email, String password, String name, String profilePictureUrl, String about) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password = password;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
        this.about = about;
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

    public String getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(String userRoles) {
        this.userRoles = userRoles;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
