package com.chatapp.synk.enums;

public enum ContactStatus {
    ADDED("Contact Added"),
    INVITED("Contact Invited"),
    CONNECTED("Contact Connected"),
    BLOCKED("Contact Blocked");

    private String description;

    ContactStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
