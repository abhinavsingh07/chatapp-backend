package com.chatapp.synk.security_validator;

import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class UserInputSanitizer {
    private static final Pattern SAFE_CHARS = Pattern.compile("[^A-Za-z0-9_-]");

    public static String sanitizeId(String id) {
        if (!StringUtils.hasText(id)) return null;
        // Allow only alphanumeric, dashes, and underscores
        return id.trim().replaceAll(SAFE_CHARS.toString(), "");
    }

    public static String sanitizeLoginId(String loginId) {
        if (!StringUtils.hasText(loginId)) return null;

        String trimmed = loginId.trim();

        // If input looks like a phone number → keep digits, +, -
        if (trimmed.matches("^[0-9+\\-\\s]+$")) {
            return trimmed.replaceAll("[^0-9+\\-]", ""); // strip spaces and junk
        }

        // Else treat as email → lowercase, strip unsafe chars
        return trimmed.toLowerCase().replaceAll("[<>\"'%;()&+ ]", "");
    }

    public static String sanitizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) return null;
        // Keep only digits, plus (+), and dashes
        return phoneNumber.trim().replaceAll("[^0-9+\\-]", "");
    }

    public static String sanitizeEmail(String email) {
        if (!StringUtils.hasText(email)) return null;
        // Lowercase, trim, and remove dangerous characters
        return email.trim().toLowerCase().replaceAll("[<>\"'%;()&+]", "");
    }

    public static String sanitizePassword(String password) {
        if (!StringUtils.hasText(password)) return null;
        // Trim only (do not alter chars, users may use special symbols)
        return password.trim();
    }

    public static String sanitizeName(String name) {
        if (!StringUtils.hasText(name)) return null;
        // Allow only letters, spaces, dots, and dashes
        return name.trim().replaceAll("[^a-zA-Z\\s.-]", "");
    }

    public static String sanitizeProfilePictureUrl(String url) {
        if (!StringUtils.hasText(url)) return null;
        // Remove spaces, dangerous chars, and only keep http/https links
        String clean = url.trim().replaceAll("[\"'<> ]", "");
        if (!clean.startsWith("http")) {
            return null; // reject invalid URL
        }
        return clean;
    }

    public static String sanitizeAbout(String about) {
        if (!StringUtils.hasText(about)) return null;
        // Basic XSS cleanup: remove <, >, "
        return about.trim().replaceAll("[<>\"']", "");
    }

    public static String sanitizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return "random"; // default fallback
        }

        // Trim whitespace
        String cleaned = alias.trim();

        // Remove dangerous characters (everything not allowed)
        cleaned = SAFE_CHARS.matcher(cleaned).replaceAll("");

        // Prevent very long values
        if (cleaned.length() > 30) {
            cleaned = cleaned.substring(0, 30);
        }

        // If it becomes empty after cleaning, fallback
        if (cleaned.isEmpty()) {
            return "random";
        }

        return cleaned;
    }

    private String sanitizeClientType(String clientType) {
        if (!StringUtils.hasText(clientType)) return "web";
        String clean = clientType.trim().toLowerCase();
        return switch (clean) {
            case "web", "mobile", "api" -> clean;
            default -> "web";
        };
    }

    public static String sanitizeUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String url = input.trim();

        // Disallow suspicious protocols (XSS vector)
        String lower = url.toLowerCase();
        if (lower.startsWith("javascript:") ||
                lower.startsWith("data:") ||
                lower.startsWith("vbscript:") ||
                lower.startsWith("file:")) {
            throw new SecurityException("Invalid or unsafe URL protocol");
        }

        try {
            URL parsed = new URL(url);

            String protocol = parsed.getProtocol().toLowerCase();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new SecurityException("Only HTTP/HTTPS URLs are allowed");
            }

            // You may also restrict hostnames if required (e.g., only your domain/CDN)
            // if (!parsed.getHost().endsWith("mydomain.com")) {
            //     throw new SecurityException("Untrusted URL host");
            // }

            // Rebuild normalized URL
            return parsed.toExternalForm();

        } catch (MalformedURLException e) {
            throw new SecurityException("Malformed URL: " + e.getMessage());
        }
    }

    public static String sanitizeText(String text) {
        if (!StringUtils.hasText(text)) return null;

        // Trim whitespace
        String cleaned = text.trim();

        // Remove obvious XSS injection characters
        cleaned = cleaned.replaceAll("[<>\"']", "");

        // Optionally, collapse multiple spaces/newlines
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        cleaned = cleaned.replaceAll("[\\r\\n]{3,}", "\n\n"); // allow newlines but limit spam

        // Prevent extremely long spammy messages
        if (cleaned.length() > 2000) {
            cleaned = cleaned.substring(0, 2000);
        }

        return cleaned;
    }
}
