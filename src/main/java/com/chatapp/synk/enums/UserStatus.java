package com.chatapp.synk.enums;

public enum UserStatus {
    ACTIVE("Active User"),
    DEACTIVATED("Deactivated User");

    private String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
