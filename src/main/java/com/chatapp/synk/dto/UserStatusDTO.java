package com.chatapp.synk.dto;

public class UserStatusDTO {
    private String userId;
    private boolean online;
    private String lastActive;

    public UserStatusDTO(String userId, boolean online, String lastActive) {
        this.userId = userId;
        this.online = online;
        this.lastActive = lastActive;
    }

    // Getters & Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getLastActive() {
        return lastActive;
    }

    public void setLastActive(String lastActive) {
        this.lastActive = lastActive;
    }
}
