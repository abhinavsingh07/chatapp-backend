package com.chatapp.synk.security;

import java.io.Serializable;

public class JwtResponse implements Serializable {
    private static final long serialVersionUID = -8091879091924046844L;
    private String id;
    private final String jwtToken;
    private String refreshToken;
    private String username;//contains phone number.
    private String name;
    private String roles;
    private String email;
    private String profilePictureUrl;
    //private Date expiry; //to do

    public JwtResponse(String jwtToken, String username, String name, String roles, String email, String profilePictureUrl, String id) {
        this.jwtToken = jwtToken;
        this.username = username;
        this.name = name;
        this.roles = roles;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.id = id;
        //this.expiry = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10);
    }

    public JwtResponse(String jwtToken, String refreshToken, String username, String name, String roles, String email, String profilePictureUrl, String id) {
        this.jwtToken = jwtToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.name = name;
        this.roles = roles;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.id = id;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
