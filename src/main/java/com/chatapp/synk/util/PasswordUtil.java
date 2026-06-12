package com.chatapp.synk.util;

public class PasswordUtil {
       public static boolean isStrongPassword(String password) {
        return password != null
                && password.length() >= 8
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit);
    }
}
