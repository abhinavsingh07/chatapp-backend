package com.chatapp.synk.util;

public class StringUtil {
    public static boolean isEmpty(final String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
