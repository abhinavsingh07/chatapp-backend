package com.chatapp.synk.security_validator;

import java.util.regex.Pattern;
public class SecurityValidationService {

    // Common attack patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(?i).*(\\bunion\\b|\\bselect\\b|\\binsert\\b|\\bupdate\\b|\\bdelete\\b|\\bdrop\\b|\\bcreate\\b|\\balter\\b|--|;|').*");

    private static final Pattern XSS_PATTERN = Pattern.compile("(?i).*(<script|javascript:|onload=|onerror=|onclick=|eval\\().*");

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*(\\.\\.[/\\\\]).*");

    public static SecurityValidationResult validateInput(String value, int maxLength, boolean allowSpecialChars) {
        if (value == null || value.trim().isEmpty()) {
            return SecurityValidationResult.valid();
        }

        String trimmed = value.trim();

        // 1. Max length check
        if (trimmed.length() > maxLength) {
            return SecurityValidationResult.invalid("Input exceeds max length " + maxLength);
        }

        // 2. SQL Injection check
        if (SQL_INJECTION_PATTERN.matcher(trimmed).matches()) {
            return SecurityValidationResult.invalid("Possible SQL Injection detected");
        }

        // 3. XSS check
        if (XSS_PATTERN.matcher(trimmed).matches()) {
            return SecurityValidationResult.invalid("Possible XSS detected");
        }

        // 4. Path Traversal check
        if (PATH_TRAVERSAL_PATTERN.matcher(trimmed).matches()) {
            return SecurityValidationResult.invalid("Path traversal attempt detected");
        }

        // 5. Special char check if disabled
        if (!allowSpecialChars && !trimmed.matches("^[a-zA-Z0-9._@-]+$")) {
            return SecurityValidationResult.invalid("Input contains forbidden special characters");
        }

        return SecurityValidationResult.valid();
    }

    public boolean isSuspicious(String value) {
        if (value == null) return false;
        return SQL_INJECTION_PATTERN.matcher(value).matches()
                || XSS_PATTERN.matcher(value).matches()
                || PATH_TRAVERSAL_PATTERN.matcher(value).matches();
    }
}
