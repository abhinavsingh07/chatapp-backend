package com.chatapp.synk.security_validator;

public class SecurityValidationResult {
    private final boolean valid;
    private final String reason;

    private SecurityValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public static SecurityValidationResult valid() {
        return new SecurityValidationResult(true, null);
    }

    public static SecurityValidationResult invalid(String reason) {
        return new SecurityValidationResult(false, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }
}
