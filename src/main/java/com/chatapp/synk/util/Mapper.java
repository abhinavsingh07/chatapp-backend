package com.chatapp.synk.util;

import com.chatapp.synk.dto.ContactDTO;
import com.chatapp.synk.dto.ConversationDTO;
import com.chatapp.synk.dto.ConversationParticipantDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.entity.Contact;
import com.chatapp.synk.entity.Conversation;
import com.chatapp.synk.entity.ConversationParticipant;
import com.chatapp.synk.entity.User;
import com.chatapp.synk.enums.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

public class Mapper {

    private static final String ALIAS_USER = "USER";

    private static final String ALIAS_CONTACT = "CONT";

    private static final String ALIAS_CONVERSATION = "CONV";

    private static final String ALIAS_PARTICIPANT = "PART";

    public static UserDTO mapToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPassword(user.getPassword());
        dto.setName(user.getName());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setAbout(user.getAbout());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus());
        return dto;
    }

    public static User mapToUserEntity(UserDTO dto, PasswordEncoder passwordEncoder) {
        String generatedId = RandomUUIDGenerater.getId(ALIAS_USER).toString();
        User user = new User();
        user.setId(generatedId);
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setProfilePictureUrl(dto.getProfilePictureUrl());
        user.setAbout(dto.getAbout());
        user.setEmail(dto.getEmail());
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public static Contact mapToContactEntity(ContactDTO dto) {
        String generatedId = RandomUUIDGenerater.getId(ALIAS_CONTACT).toString();
        Contact contact = new Contact();
        contact.setId(generatedId);
        contact.setUserId(dto.getUserId());
        contact.setContactUserId(dto.getContactUserId());
        contact.setContactStatus(dto.getContactStatus());
        contact.setEmailStatus(dto.getEmailStatus());
        contact.setEmail(dto.getEmail());
        return contact;
    }

    public static ContactDTO mapToContactDTO(Contact contact) {
        ContactDTO dto = new ContactDTO();
        dto.setId(contact.getId());
        dto.setUserId(contact.getUserId());
        dto.setContactUserId(contact.getContactUserId());
        dto.setContactStatus(contact.getContactStatus());
        dto.setEmailStatus(contact.getEmailStatus());
        dto.setEmail(contact.getEmail());
        return dto;
    }


    public static Conversation mapToConversationEntity(ConversationDTO dto) {
        Conversation conversation = new Conversation();
        conversation.setId(RandomUUIDGenerater.getId(ALIAS_CONVERSATION).toString());
        conversation.setConversationType(dto.getConversationType());
        return conversation;
    }

    public static ConversationDTO mapToConversationDTO(Conversation entity) {
        return new ConversationDTO(entity.getId(), entity.getConversationType());
    }

    public static ConversationParticipant mapToParticipantEntity(ConversationParticipantDTO dto) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setId(RandomUUIDGenerater.getId(ALIAS_PARTICIPANT).toString());
        participant.setConversationId(dto.getConversationId());
        participant.setUserId(dto.getUserId());
        return participant;
    }

    public static ConversationParticipantDTO mapToParticipantDTO(ConversationParticipant entity) {
        return new ConversationParticipantDTO(entity.getId(), entity.getConversationId(), entity.getUserId());
    }
}
