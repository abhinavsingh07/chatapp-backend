package com.chatapp.synk.security_validator;

public class InputSecurityUtils {

    //this class is doing all checks validating format , validate for security checks and sanitizing

    /**
     * Validate + Sanitize ID
     */
    public static String secureId(String id) {
        if (id == null) return null;
        checkField(id, 100, true, "id");
        if (!UserInputValidator.isValidId(id)) {
            throw new SecurityException("Invalid ID format");
        }
        return UserInputSanitizer.sanitizeId(id);
    }

    /**
     * Validate + Sanitize Phone or Email (loginId)
     */
    public static String secureLoginId(String loginId) {
        if (loginId == null) return null;
        checkField(loginId, 50, true, "loginId");
        if (!UserInputValidator.isValidLoginId(loginId.trim())) {
            throw new SecurityException("Invalid login identifier (must be phone or email)");
        }
        return UserInputSanitizer.sanitizeLoginId(loginId);
    }

    /**
     * Validate + Sanitize Phone
     */
    public static String securePhone(String phone) {
        if (phone == null) return null;
        checkField(phone, 15, false, "phoneNumber");
        if (!UserInputValidator.isValidPhoneNumber(phone)) {
            throw new SecurityException("Invalid phone number");
        }
        return UserInputSanitizer.sanitizePhoneNumber(phone);
    }

    /**
     * Validate + Sanitize Email
     */
    public static String secureEmail(String email) {
        if (email == null) return null;
        checkField(email, 100, true, "email");
        if (!UserInputValidator.isValidEmail(email)) {
            throw new SecurityException("Invalid email address");
        }
        return UserInputSanitizer.sanitizeEmail(email);
    }

    /**
     * Validate + Sanitize Name
     */
    public static String secureName(String name) {
        if (name == null) return null;
        checkField(name, 50, true, "name");
        return UserInputSanitizer.sanitizeName(name);
    }

    /**
     * Validate + Sanitize Password (validation only, no alteration)
     */
    public static String securePassword(String password) {
        if (password == null) return null;
        checkField(password, 200, true, "password");
        if (password.length() < 8) {
            throw new SecurityException("Password must be at least 8 characters");
        }
        return UserInputSanitizer.sanitizePassword(password);
    }

    /**
     * Validate + Sanitize About field
     */
    public static String secureAbout(String about) {
        if (about == null) return null;
        checkField(about, 500, true, "about");
        return UserInputSanitizer.sanitizeAbout(about);
    }

    /**
     * Validate + Sanitize URL
     */
    public static String secureUrl(String url) {
        if (url == null) return null;
        checkField(url, 200, true, "url");
        return UserInputSanitizer.sanitizeUrl(url);
    }

    public static String secureMessage(String message) {
        if (message == null) return null;

        // Validation: check length and whether empty text allowed
        checkField(message, 1000, true, "message"); // max length = 1000 chars, change if needed

        // Sanitization: remove dangerous chars / patterns
        return UserInputSanitizer.sanitizeText(message);
    }

    /**
     * Reusable security risk checker
     */
    private static void checkField(String value, int maxLength, boolean allowSpecial, String fieldName) {
        SecurityValidationResult result = SecurityValidationService.validateInput(value, maxLength, allowSpecial);
        if (!result.isValid()) {
            throw new SecurityException(fieldName + " validation failed: " + result.getReason());
        }
    }

}
