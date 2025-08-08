package com.chatapp.synk.service;

import com.chatapp.synk.dto.ContactDTO;
import com.chatapp.synk.dto.ContactUserDTO;

import java.util.List;

public interface ContactService {
    ContactDTO addContact(ContactDTO contactDTO);

    List<ContactDTO> getContactsByUserId(String userId);

    public List<ContactUserDTO> getContacts(String userId);

    void deleteContact(String contactId);
}
