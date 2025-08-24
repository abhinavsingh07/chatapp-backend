package com.chatapp.synk.security_validator;

import java.util.UUID;
import java.util.regex.Pattern;

public class UserInputValidator {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,30}$");

    public static boolean isValidLoginId(String loginId) {
        if (loginId == null || loginId.isEmpty()) return false;
        String trimmed = loginId.trim();
        return PHONE_PATTERN.matcher(trimmed).matches() || EMAIL_PATTERN.matcher(trimmed).matches();
    }

    public static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) return false;

        String[] parts = id.split("_", 2);
        if (parts.length != 2) return false;

        // Validate UUID
        try {
            UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            return false;
        }

        // Validate alias
        return ALIAS_PATTERN.matcher(parts[1]).matches();
    }

    public static boolean isValidPhoneNumber(String input) {
        return input.matches("^\\d{10}$"); // Adjust pattern if needed
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
