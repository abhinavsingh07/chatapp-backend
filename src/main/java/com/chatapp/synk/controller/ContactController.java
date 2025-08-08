package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ContactDTO;
import com.chatapp.synk.dto.ContactUserDTO;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ContactService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    @Autowired
    private ContactService contactService;

    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<ContactUserDTO>> getContacts(@RequestParam(required = false) String userId) {
        logger.info("Fetching contacts for userId: {}", userId);

        List<ContactUserDTO> contactUserDTOList = contactService.getContacts(userId);

        if (contactUserDTOList.isEmpty()) {
            logger.warn("No contacts found for userId: {}", userId);
            return ResponseEntity.ok(new SuccessResponse<>("404", "No contacts found", Collections.emptyList()));
        } else {
            logger.info("Found {} contacts for userId: {}", contactUserDTOList.size(), userId);
            return ResponseEntity.ok(new SuccessResponse<>("200", "Contacts fetched successfully", contactUserDTOList));
        }
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<ContactDTO>> addContact(@Valid @RequestBody ContactDTO contactDTO) {
        logger.info("Creating contact for given userid..");
        ContactDTO savedContact = contactService.addContact(contactDTO);
        String message = savedContact.getContactStatus() == ContactStatus.ADDED ? "Contact created successfully" : "Invitation sent successfully";
        return ResponseEntity.ok(new SuccessResponse<>("200", message, List.of(savedContact)));
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<SuccessResponse<String>> deleteContact(@PathVariable String contactId) {
        logger.info("Deleting contact with userId: {}", contactId);
        contactService.deleteContact(contactId);
        return ResponseEntity.ok(new SuccessResponse<>("200", "Contact deleted successfully", List.of()));
    }

}