package com.chatapp.synk.service.impl;

import com.api.emailservice.EmailDTO;
import com.api.emailservice.EmailService;
import com.chatapp.synk.dto.ContactDTO;
import com.chatapp.synk.dto.ContactUserDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.entity.Contact;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.EmailStatus;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.repository.ContactRepository;
import com.chatapp.synk.service.ContactService;
import com.chatapp.synk.service.UserService;
import com.chatapp.synk.util.AppUtils;
import com.chatapp.synk.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
    @Autowired
    private ExecutorService taskExecutor;

    @Override
    @Cacheable(value = "contactListCache", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<ContactDTO> getContactsByUserId(String userId) {
        logger.info("Fetching contacts for userId: {}", userId);
        List<Contact> contactList = contactRepository.findAllByUserId(userId);
        logger.info("Returning {} Contact for userId {}", contactList.size(), userId);
        return contactList.stream().filter(Objects::nonNull).map(Mapper::mapToContactDTO).collect(Collectors.toList());

    }

    @Override
    @Cacheable(value = "contactListCache", key = "#userId != null && !#userId.isEmpty() ? #userId : 'ALL_CONTACTS'", unless = "#result == null || #result.isEmpty()")
    public List<ContactUserDTO> getContacts(String userId) {
        if (userId != null && !userId.isEmpty()) {
            logger.info("Fetching contacts for userId: {}", userId);
            return contactRepository.findContactUserDetailsByUserId(userId);
        } else {
            logger.info("Fetching all contacts with user mapping");
            return contactRepository.findAllContactsWithUserDetails();
        }
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
        String userId = contact.getUserId();
        contactRepository.delete(contact);

        // manual eviction using local variable
        cacheManager.getCache("contactListCache").evict(userId);
        logger.info("Contact with ID {} deleted successfully", contactId);
    }

    @Override
    //@CachePut(value = "contactCache", key = "#result.id", unless = "#result == null")
    public ContactDTO addContact(ContactDTO dto) {
        String userId = dto.getUserId();
        String email = dto.getEmail();
        logger.info("Adding contact for userId={} by email={}", userId, email);

        if (!StringUtils.hasText(userId) || !StringUtils.hasText(email)) {
            throw new ServiceException("UserId and email are required", HttpStatus.BAD_REQUEST);
        }

        if (!AppUtils.isValidEmail(email)) {
            throw new ServiceException("Invalid email format", HttpStatus.BAD_REQUEST);
        }

        try {
            // Check if user exists for the given email
            UserDTO existingUser = userService.getUserByPhoneNumberOrEmail(email);

            // Prevent adding self as contact
            if (existingUser.getId().equals(userId)) {
                throw new ServiceException("You cannot add yourself as a contact", HttpStatus.BAD_REQUEST);
            }

            // Prevent duplicate contact
            if (contactExists(userId, existingUser.getId())) {
                throw new ServiceException("Contact already exists for this user", HttpStatus.BAD_REQUEST);
            }

            // Registered user flow
            ContactDTO contactDTO = new ContactDTO();
            contactDTO.setUserId(userId);
            contactDTO.setContactUserId(existingUser.getId());//contact userid setting
            contactDTO.setContactStatus(ContactStatus.ADDED);
            contactDTO.setEmailStatus(EmailStatus.NOT_APPLICABLE);
            contactDTO.setEmail(email);

            return saveContact(contactDTO);

        } catch (ServiceException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                // Invite flow
                return handleInviteFlow(userId, email);
            }
            throw ex;
        }
    }

    private ContactDTO handleInviteFlow(String userId, String email) {
        logger.info("No registered user found for email={}, sending invite...", email);

        // Save contact first
        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setUserId(userId);
        contactDTO.setContactUserId(null);//contact user id is null for invite
        contactDTO.setContactStatus(ContactStatus.INVITED);
        contactDTO.setEmailStatus(EmailStatus.PENDING);
        contactDTO.setEmail(email);
        ContactDTO savedContact = saveContact(contactDTO);

        // Send invite asynchronously
        CompletableFuture.runAsync(() -> {
            boolean sent = emailService.sendEmail(new EmailDTO(email, "You're invited to join ChatApp!", "Hi there!\n\nYou've been invited to join ChatApp. " + "Click here to register:\nhttps://yourapp.com/register"));
            updateEmailStatus(savedContact.getId(), sent ? EmailStatus.SENT : EmailStatus.FAILED);
        }, taskExecutor);//use our thread pool not fork join pool

        return savedContact;
    }

    private boolean contactExists(String userId, String contactUserId) {
        return !contactRepository.findByUserIdAndContactUserId(userId, contactUserId).isEmpty();
    }

    private void updateEmailStatus(String contactId, EmailStatus status) {
        contactRepository.findById(contactId).ifPresent(contact -> {
            contact.setEmailStatus(status);
            contact.setContactStatus(ContactStatus.ADDED);
            contactRepository.save(contact);
        });
    }

    private ContactDTO saveContact(ContactDTO contactDTO) {
        try {
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
