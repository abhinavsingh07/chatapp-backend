package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ContactDTO;
import com.chatapp.synk.dto.ContactUserDTO;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ContactService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<ContactUserDTO>> getContacts(@RequestParam(required = false) String userId) {
        logger.info("Fetching contacts for userId: {}", userId);

        List<ContactUserDTO> contactUserDTOList = contactService.getContacts(userId);

        if (contactUserDTOList.isEmpty()) {
            logger.warn("No contacts found for userId: {}", userId);
            return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.NOT_FOUND, "No contacts found", Collections.emptyList()));
        }

        logger.info("Found {} contacts for userId: {}", contactUserDTOList.size(), userId);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "Contacts fetched successfully", contactUserDTOList));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<ContactDTO>> addContact(@Valid @RequestBody ContactDTO contactDTO) {
        logger.debug("Request received to add contact for userId: {}", contactDTO.getUserId());

        ContactDTO savedContact = contactService.addContact(contactDTO);
        String message = savedContact.getContactStatus() == ContactStatus.ADDED ? "Contact created successfully" : "Invitation sent successfully";

        logger.info("Contact action [{}] completed for userId: {}", message, contactDTO.getUserId());
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, message, List.of(savedContact)));
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<SuccessResponse<String>> deleteContact(@PathVariable String contactId) {
        logger.info("Deleting contact with contactId: {}", contactId);

        contactService.deleteContact(contactId);

        logger.info("Contact deleted successfully for contactId: {}", contactId);
        return ResponseEntity.ok(new SuccessResponse<>(HttpStatus.OK, "Contact deleted successfully", List.of()));
    }
}
