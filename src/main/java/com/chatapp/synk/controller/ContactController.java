package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ContactDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.response.SuccessResponse;
import com.chatapp.synk.service.ContactService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    private final ContactService contactService;

    @Autowired
    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<SuccessResponse<UserDTO>> getContactsByUserId(@PathVariable(required = true) String userId) {
        logger.info("Received contact search request for userId: {}", userId);

        List<UserDTO> results = contactService.getContactsByUserId(userId);

        if (results.isEmpty()) {
            logger.warn("No contacts found for userId: {}", userId);
        } else {
            logger.info("Found {} contacts for userId: {}", results.size(), userId);
        }

        String msg = results.isEmpty() ? "No contacts found" : "Contact list retrieved";
        String code = results.isEmpty() ? "404" : "200";

        return ResponseEntity.ok(new SuccessResponse<>(code, msg, results));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<ContactDTO>> addContact(@Valid @RequestBody ContactDTO contactDTO) {
        logger.info("Creating contact for given userid..");
        ContactDTO contact = contactService.addContact(contactDTO);
        return ResponseEntity.ok(new SuccessResponse<>("200", "Contact created successfully", List.of(contact)));
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<SuccessResponse<String>> deleteContact(@PathVariable String contactId) {
        logger.info("Deleting contact with userId: {}", contactId);
        contactService.deleteContact(contactId);
        return ResponseEntity.ok(new SuccessResponse<>("200", "Contact deleted successfully", List.of()));
    }

    @PostMapping("/add-by-email/{userId}/{contactEmail}")
    public ResponseEntity<SuccessResponse<ContactDTO>> addContactByEmail(@PathVariable String userId, @PathVariable String contactEmail)  {

        logger.info("Request received to add contact by email: {} for userId: {}", contactEmail, userId);

        ContactDTO contactDTO = contactService.addContactEmailFlow(userId, contactEmail);

        String message = contactDTO.getStatus() == ContactStatus.ADDED ? "Contact added successfully." : "User not registered. Invitation sent, contact saved as invited.";

        SuccessResponse<ContactDTO> response = new SuccessResponse<>("200", message, List.of(contactDTO));

        return ResponseEntity.ok(response);
    }


}