package com.chatapp.synk.util;

public class MaskIdentifierUtil {

    public static String maskIdentifier(String identifier) {
        if (identifier == null)
            return "null";
        // mask email/phone for logs: e.g., 98****1234 or j***@domain.com
        if (identifier.contains("@")) {
            int idx = identifier.indexOf("@");
            return identifier.charAt(0) + "***" + identifier.substring(idx);
        } else if (identifier.length() > 4) {
            return identifier.substring(0, 2) + "****" + identifier.substring(identifier.length() - 2);
        }
        return "****";
    }
}
