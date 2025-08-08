package com.chatapp.synk.service.impl;

import com.api.emailservice.EmailDTO;
import com.api.emailservice.EmailService;
import com.chatapp.synk.dto.ContactDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.entity.Contact;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.EmailStatus;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.repository.ContactRepository;
import com.chatapp.synk.service.ContactService;
import com.chatapp.synk.service.UserService;
import com.chatapp.synk.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {
    private static final Logger logger = LoggerFactory.getLogger(ContactServiceImpl.class);
    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private EmailService emailService;

    @Override
    @CachePut(value = "contactCache", key = "#result.id", unless = "#result == null")
    public ContactDTO addContact(ContactDTO contactDTO) {
        logger.info("Attempting to add contact: userId={}, contactUserId={}", contactDTO.getUserId(), contactDTO.getContactUserId());

        List<Contact> existingContact = contactRepository.findByUserIdAndContactUserId(contactDTO.getUserId(), contactDTO.getContactUserId());

        if (existingContact.size() > 0) {
            logger.warn("Contact already exists for userId={} and contactUserId={}", contactDTO.getUserId(), contactDTO.getContactUserId());
            throw new ServiceException("Contact already exists for this user. Please try again with a different contact.", HttpStatus.BAD_REQUEST);
        }

        logger.debug("No existing contact found. Proceeding to save new contact.");

        ContactDTO savedContactDTO = saveContact(contactDTO);
        logger.info("Contact saved successfully with ID: {}", savedContactDTO.getId());
        return savedContactDTO;
    }


    @Override
    @Cacheable(value = "contactListCache", key = "#userId")
    public List<UserDTO> getContactsByUserId(String userId) {
        logger.info("Fetching contacts for userId: {}", userId);

        List<Contact> contactList = contactRepository.findAllByUserId(userId);
        logger.debug("Found {} contacts for userId {}", contactList.size(), userId);
        //we are not using hibernate mapping explicitly we are fetching relations.
        List<UserDTO> userDTOList = contactList.stream().map(contact -> {
            String contactUserId = contact.getContactUserId();
            logger.debug("Fetching UserDTO for contactUserId: {}", contactUserId);
            UserDTO userDTO = userService.getUserById(contactUserId);
            if (userDTO == null) {
                logger.warn("UserDTO not found for contactUserId: {}", contactUserId);
            }
            return userDTO;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        logger.info("Returning {} UserDTOs for userId {}", userDTOList.size(), userId);
        return userDTOList;
    }

    @Override
    @Caching(evict = {@CacheEvict(value = "contactCache", key = "#contactId")})
    public void deleteContact(String contactId) {
        logger.info("Attempting to delete contact with ID: {}", contactId);

        Optional<Contact> contactOpt = contactRepository.findById(contactId);
        if (contactOpt.isEmpty()) {
            logger.warn("No contact found with ID: {}", contactId);
            throw new ServiceException("Contact not found for given contact id", HttpStatus.NOT_FOUND);
        }

        Contact contact = contactOpt.get();
        contactRepository.delete(contact);
        // manual eviction using local variable
        String userId = contact.getUserId();
        cacheManager.getCache("contactListCache").evict(userId);
        logger.info("Contact with ID {} deleted successfully", contactId);
    }


    public ContactDTO addContactEmailFlow(String userId, String email) {
        logger.info("Attempting to add contact by email: {}", email);

        if (userId == null || email == null || email.isBlank()) {
            throw new ServiceException("User ID and email are required", HttpStatus.BAD_REQUEST);
        }

        try {
            // Check if user exists
            UserDTO userDTO = userService.getUserByPhoneNumberOrEmail(email);

            // Existing user → Add as contact
            ContactDTO contactDTO = new ContactDTO();
            contactDTO.setUserId(userId);
            contactDTO.setContactUserId(userDTO.getId());
            contactDTO.setStatus(ContactStatus.ADDED);
            //contactDTO.setEmailStatus(EmailStatus.NOT_APPLICABLE);
            return saveContact(contactDTO);

        } catch (ServiceException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                // No existing user → Invite via email
                ContactDTO contactDTO = new ContactDTO();
                contactDTO.setUserId(userId);
                contactDTO.setStatus(ContactStatus.INVITED);
                contactDTO.setEmailStatus(EmailStatus.PENDING);

                ContactDTO savedContact = saveContact(contactDTO);
                //this whole is blocking call we need to take out as async call
                EmailDTO emailDTO = new EmailDTO(email, "You're invited to join ChatApp!", "Hi there!\n\nYou've been invited to join ChatApp. " + "Click the link below to register:\nhttps://yourapp.com/register");

                boolean sent = emailService.sendEmail(emailDTO);
                savedContact.setEmailStatus(sent ? EmailStatus.SENT : EmailStatus.FAILED);

                // Update with email status
                saveContact(savedContact);
                //this whole is blocking call we need to take out as async call

                return savedContact;
            } else {
                throw ex;
            }
        }
    }


    private ContactDTO saveContact(ContactDTO contactDTO) {
        try {
            if (contactDTO.getUserId() == null) {
                throw new ServiceException("Invalid contact details", HttpStatus.BAD_REQUEST);
            }
            logger.info("Saving contact for userId: {}", contactDTO.getUserId());
            Contact contactEntity = Mapper.mapToContactEntity(contactDTO);
            Contact saved = contactRepository.save(contactEntity);
            return Mapper.mapToContactDTO(saved);

        } catch (Exception ex) {
            logger.error("Error while saving contact: {}", ex.getMessage());
            throw new ServiceException("Failed to save contact", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
