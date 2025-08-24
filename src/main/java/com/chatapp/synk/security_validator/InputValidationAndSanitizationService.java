package com.chatapp.synk.security_validator;

import com.chatapp.synk.dto.*;
import com.chatapp.synk.enums.ConversationType;

public class InputValidationAndSanitizationService {

    public static AuthDTO validateAndSanitize(AuthDTO authDTO) throws SecurityException {
        if (authDTO == null) return null;

        // 1. Validate first (reject malicious inputs)
        validateForSecurityThreats(authDTO);

        // 2. Sanitize accepted inputs
        AuthDTO sanitized = new AuthDTO();
        sanitized.setPhoneNumberOrEmail(UserInputSanitizer.sanitizeLoginId(authDTO.getPhoneNumberOrEmail()));
        sanitized.setPassword(authDTO.getPassword()); // never alter password chars
        return sanitized;
    }

    private static void validateForSecurityThreats(AuthDTO authDTO) {
        // Step 1: Generic security checks
        checkFieldForPotentialSecurityRisks(authDTO.getPhoneNumberOrEmail(), 50, true, "phoneNumberOrEmail");
        checkFieldForPotentialSecurityRisks(authDTO.getPassword(), 100, true, "password");

        // Step 2: Business-specific checks
        if (authDTO.getPhoneNumberOrEmail() == null) {
            throw new SecurityException("phoneNumberOrEmail is required");
        }
        if (!UserInputValidator.isValidLoginId(authDTO.getPhoneNumberOrEmail().trim())) {
            throw new SecurityException("phoneNumberOrEmail must be a valid phone number or email");
        }

        // Password-specific checks
        if (authDTO.getPassword() == null || authDTO.getPassword().length() < 8) {
            throw new SecurityException("Password must be at least 8 characters");
        }
        if (authDTO.getPassword().length() > 200) {
            throw new SecurityException("Password too long");
        }
    }


    public static UserDTO validateAndSanitize(UserDTO userDTO) throws SecurityException {
        if (userDTO == null) return null;

        // 1. Validate
        validateForSecurityThreats(userDTO);

        // 2. Sanitize
        UserDTO sanitized = new UserDTO();
        sanitized.setId(UserInputSanitizer.sanitizeId(userDTO.getId()));
        sanitized.setPhoneNumber(UserInputSanitizer.sanitizePhoneNumber(userDTO.getPhoneNumber()));
        sanitized.setEmail(UserInputSanitizer.sanitizeEmail(userDTO.getEmail()));
        sanitized.setPassword(userDTO.getPassword()); // never alter password
        sanitized.setName(UserInputSanitizer.sanitizeName(userDTO.getName()));
        sanitized.setProfilePictureUrl(UserInputSanitizer.sanitizeUrl(userDTO.getProfilePictureUrl()));
        sanitized.setAbout(UserInputSanitizer.sanitizeAbout(userDTO.getAbout()));

        return sanitized;
    }

    private static void validateForSecurityThreats(UserDTO userDTO) {
        // Generic validation (length, nulls, injection patterns)
        checkFieldForPotentialSecurityRisks(userDTO.getId(), 100, true, "id");
        checkFieldForPotentialSecurityRisks(userDTO.getPhoneNumber(), 15, false, "phoneNumber");
        checkFieldForPotentialSecurityRisks(userDTO.getEmail(), 100, true, "email");
        checkFieldForPotentialSecurityRisks(userDTO.getPassword(), 200, true, "password");
        checkFieldForPotentialSecurityRisks(userDTO.getName(), 50, true, "name");
        checkFieldForPotentialSecurityRisks(userDTO.getProfilePictureUrl(), 200, true, "profilePictureUrl");
        checkFieldForPotentialSecurityRisks(userDTO.getAbout(), 500, true, "about");

        // Business-specific rules
        if (userDTO.getPhoneNumber() == null || !UserInputValidator.isValidPhoneNumber(userDTO.getPhoneNumber())) {
            throw new SecurityException("Invalid phone number");
        }
        if (userDTO.getEmail() == null || !UserInputValidator.isValidEmail(userDTO.getEmail())) {
            throw new SecurityException("Invalid email address");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().length() < 8) {
            throw new SecurityException("Password must be at least 8 characters");
        }
        if (userDTO.getName() == null || userDTO.getName().trim().isEmpty()) {
            throw new SecurityException("Name is required");
        }
    }

    public static ContactDTO validateAndSanitize(ContactDTO contactDTO) throws SecurityException {
        if (contactDTO == null) return null;

        // 1. Validate
        validateForSecurityThreats(contactDTO);

        // 2. Sanitize
        ContactDTO sanitized = new ContactDTO();
        sanitized.setId(UserInputSanitizer.sanitizeId(contactDTO.getId()));
        sanitized.setUserId(UserInputSanitizer.sanitizeId(contactDTO.getUserId()));
        sanitized.setEmail(UserInputSanitizer.sanitizeEmail(contactDTO.getEmail()));
        sanitized.setContactUserId(UserInputSanitizer.sanitizeId(contactDTO.getContactUserId()));
        sanitized.setContactStatus(contactDTO.getContactStatus()); // enum, safe
        sanitized.setEmailStatus(contactDTO.getEmailStatus());     // enum, safe
        sanitized.setUser(validateAndSanitize(contactDTO.getUser()));
        // reuse UserDTO validator/sanitizer for nested object

        return sanitized;
    }

    private static void validateForSecurityThreats(ContactDTO contactDTO) {
        // Generic validation (length, nulls, injection patterns)
        checkFieldForPotentialSecurityRisks(contactDTO.getId(), 100, true, "id");
        checkFieldForPotentialSecurityRisks(contactDTO.getUserId(), 100, true, "userId");
        checkFieldForPotentialSecurityRisks(contactDTO.getEmail(), 100, true, "email");
        checkFieldForPotentialSecurityRisks(contactDTO.getContactUserId(), 100, true, "contactUserId");

        // Business-specific rules
        if (contactDTO.getUserId() == null || contactDTO.getUserId().trim().isEmpty()) {
            throw new SecurityException("UserId is required");
        }
        if (contactDTO.getEmail() == null || !UserInputValidator.isValidEmail(contactDTO.getEmail())) {
            throw new SecurityException("Invalid email address");
        }
    }

    public static ConversationDTO validateAndSanitize(ConversationDTO conversationDTO) throws SecurityException {
        if (conversationDTO == null) return null;

        // 1. Validate
        validateForSecurityThreats(conversationDTO);

        // 2. Sanitize
        ConversationDTO sanitized = new ConversationDTO();
        sanitized.setId(UserInputSanitizer.sanitizeId(conversationDTO.getId()));
        sanitized.setConversationType(conversationDTO.getConversationType()); // enum, no sanitization needed

        return sanitized;
    }

    private static void validateForSecurityThreats(ConversationDTO conversationDTO) {
        // Generic validation
        checkFieldForPotentialSecurityRisks(conversationDTO.getId(), 100, true, "id");

        // Business-specific rules
        if (conversationDTO.getConversationType() == null) {
            throw new SecurityException("Conversation type is required");
        }

        // Ensure it's one of the allowed enum values
        try {
            ConversationType.valueOf(conversationDTO.getConversationType().toString());
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Invalid conversation type");
        }
    }


    public static ConversationParticipantDTO validateAndSanitize(ConversationParticipantDTO dto) throws SecurityException {
        if (dto == null) return null;

        // 1. Validate
        validateForSecurityThreats(dto);

        // 2. Sanitize
        ConversationParticipantDTO sanitized = new ConversationParticipantDTO();
        sanitized.setId(UserInputSanitizer.sanitizeId(dto.getId()));
        sanitized.setConversationId(UserInputSanitizer.sanitizeId(dto.getConversationId()));
        sanitized.setUserId(UserInputSanitizer.sanitizeId(dto.getUserId()));

        return sanitized;
    }

    private static void validateForSecurityThreats(ConversationParticipantDTO dto) {
        // Generic validation with field-specific max lengths
        checkFieldForPotentialSecurityRisks(dto.getId(), 100, false, "id"); // id can be optional
        checkFieldForPotentialSecurityRisks(dto.getConversationId(), 100, true, "conversationId");
        checkFieldForPotentialSecurityRisks(dto.getUserId(), 100, true, "userId");

        // Business rules
        if (dto.getConversationId() == null || dto.getConversationId().trim().isEmpty()) {
            throw new SecurityException("Conversation ID is required");
        }
        if (dto.getUserId() == null || dto.getUserId().trim().isEmpty()) {
            throw new SecurityException("User ID is required");
        }
    }

    public static MessageDTO validateAndSanitize(MessageDTO dto) throws SecurityException {
        if (dto == null) return null;

        // 1. Validate
        validateForSecurityThreats(dto);

        // 2. Sanitize
        MessageDTO sanitized = new MessageDTO();
        sanitized.setId(UserInputSanitizer.sanitizeId(dto.getId()));
        sanitized.setConversationId(UserInputSanitizer.sanitizeId(dto.getConversationId()));
        sanitized.setSenderId(UserInputSanitizer.sanitizeId(dto.getSenderId()));
        sanitized.setReceiverId(UserInputSanitizer.sanitizeId(dto.getReceiverId()));
        sanitized.setContent(UserInputSanitizer.sanitizeText(dto.getContent()));  // special for text
        sanitized.setMediaId(UserInputSanitizer.sanitizeId(dto.getMediaId()));

        return sanitized;
    }

    private static void validateForSecurityThreats(MessageDTO dto) {
        // Id fields
        checkFieldForPotentialSecurityRisks(dto.getId(), 100, false, "id");
        checkFieldForPotentialSecurityRisks(dto.getConversationId(), 100, true, "conversationId");
        checkFieldForPotentialSecurityRisks(dto.getSenderId(), 100, true, "senderId");
        checkFieldForPotentialSecurityRisks(dto.getReceiverId(), 100, true, "receiverId");
        checkFieldForPotentialSecurityRisks(dto.getMediaId(), 100, false, "mediaId");

        // Message content - could be optional but should be checked for XSS
        // allow special should be true as user can enter ?,@,#,$ any special character
        // and we are checking for security vulnerability.
        checkFieldForPotentialSecurityRisks(dto.getContent(), 2000, true, "content");


        // Required field validation
        if (dto.getConversationId() == null || dto.getConversationId().trim().isEmpty()) {
            throw new SecurityException("Conversation ID is required");
        }
        if (dto.getSenderId() == null || dto.getSenderId().trim().isEmpty()) {
            throw new SecurityException("Sender ID is required");
        }
        if (dto.getReceiverId() == null || dto.getReceiverId().trim().isEmpty()) {
            throw new SecurityException("Receiver ID is required");
        }
    }

    private static void checkFieldForPotentialSecurityRisks(String value, int maxLength, boolean allowSpecial, String fieldName) {
        if (value == null) return;
        //security validator check for potential security risks.
        SecurityValidationResult result = SecurityValidationService.validateInput(value, maxLength, allowSpecial);
        if (!result.isValid()) {
            throw new SecurityException(fieldName + " validation failed: " + result.getReason());
        }
    }
}
