package com.chatapp.synk.enums;

public enum EmailStatus {

    PENDING("Pending"),
    SENT("Sent"),
    FAILED("Failed"),
    NOT_APPLICABLE("Not applicable");

    private final String description;

    EmailStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    }
