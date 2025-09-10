package com.chatapp.synk.service.impl;

import com.api.emailservice.EmailDTO;
import com.api.emailservice.EmailService;
import com.chatapp.synk.security_validator.InputSecurityUtils;
import com.chatapp.synk.security_validator.InputValidationAndSanitizationService;
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
import com.chatapp.synk.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {
    private static final Logger logger = LoggerFactory.getLogger(ContactServiceImpl.class);
    private final ContactRepository contactRepository;
    private final UserService userService;
    private final CacheManager cacheManager;
    private final EmailService emailService;
    private final ExecutorService taskExecutor;

    public ContactServiceImpl(ContactRepository contactRepository, UserService userService, CacheManager cacheManager, EmailService emailService, ExecutorService taskExecutor) {
        this.contactRepository = contactRepository;
        this.userService = userService;
        this.cacheManager = cacheManager;
        this.emailService = emailService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    @Cacheable(value = "contactListCache", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<ContactDTO> getContactsByUserId(String userId) {
        List<Contact> contactList = contactRepository.findAllByUserId(userId.trim());
        return contactList.stream().filter(Objects::nonNull).map(Mapper::mapToContactDTO).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "contactListCache", key = "#userId != null && !#userId.isEmpty() ? #userId : 'ALL_CONTACTS'", unless = "#result == null || #result.isEmpty()")
    public List<ContactUserDTO> getContacts(String userId) {
        String validId = InputSecurityUtils.secureId(userId);
        if (validId != null && !validId.isEmpty()) {
            return contactRepository.findContactUserDetailsByUserId(userId.trim());
        } else {
            return contactRepository.findAllContactsWithUserDetails();
        }
    }

    @Override
    @Caching(evict = {@CacheEvict(value = "contactCache", key = "#contactId", beforeInvocation = true)})
    public void deleteContact(String contactId) {
        String validId = InputSecurityUtils.secureId(contactId);
        Optional<Contact> contactOpt = contactRepository.findById(validId);

        if (contactOpt.isEmpty()) {
            logger.warn("Delete failed - no contact found with ID: {}", contactId);
            throw new ServiceException("Contact not found for given contact id", HttpStatus.NOT_FOUND);
        }

        Contact contact = contactOpt.get();
        String userId = contact.getUserId();
        String contactUserId = contact.getContactUserId();

        // delete from DB
        contactRepository.delete(contact);

        // Evict related user caches
        Cache contactListCache = cacheManager.getCache("contactListCache");
        if (contactListCache != null) {
            contactListCache.evict(userId);
            contactListCache.evict("ALL_CONTACTS");
        }

        // Evict mutual contact caches for both directions
        Cache mutualCache = cacheManager.getCache("mutualContacts");
        if (mutualCache != null) {
            String keyAB = userId + "_" + contactUserId;
            String keyBA = contactUserId + "_" + userId;
            mutualCache.evict(keyAB);
            mutualCache.evict(keyBA);
            logger.debug("Evicted mutual contact cache entries: [{}] and [{}]", keyAB, keyBA);
        }

        logger.info("Contact deleted successfully: {}", contactId);
    }


    @Override
    @Cacheable(value = "mutualContacts", key = "#userAId + '_' + #userBId")
    public boolean isMutualContact(String userAId, String userBId) {
        String validuserAId = InputSecurityUtils.secureId(userAId);
        String validuserBId = InputSecurityUtils.secureId(userBId);
        logger.debug("Checking mutual contact status between [{}] and [{}]", validuserAId, validuserAId);

        boolean aToB = contactRepository.existsByUserIdAndContactUserIdAndContactStatus(validuserAId, validuserAId, ContactStatus.ADDED);
        logger.trace("Contact check: [{} -> {}] = {}", validuserAId, validuserAId, aToB);

        boolean bToA = contactRepository.existsByUserIdAndContactUserIdAndContactStatus(validuserAId, validuserAId, ContactStatus.ADDED);
        logger.trace("Contact check: [{} -> {}] = {}", validuserAId, validuserAId, bToA);

        boolean mutual = aToB && bToA;
        logger.debug("Mutual contact result between [{}] and [{}] = {}", validuserAId, validuserAId, mutual);

        return mutual;
    }

    @Override
    @Caching(evict = {@CacheEvict(value = "contactListCache", key = "#dto.userId", condition = "#dto != null", beforeInvocation = true), @CacheEvict(value = "contactListCache", key = "'ALL_CONTACTS'", beforeInvocation = true)}, put = {@CachePut(value = "contactCache", key = "#result.id", unless = "#result == null")})
    public ContactDTO addContact(ContactDTO dto) {
        ContactDTO validDTO = InputValidationAndSanitizationService.validateAndSanitize(dto);
        String userId = validDTO.getUserId();
        String email = validDTO.getEmail();

        logger.info("Processing addContact request for userId={} with email={}", userId, email);

        try {
            UserDTO existingUser = userService.getUserByPhoneNumberOrEmail(email);

            if (existingUser.getId().equals(userId)) {
                throw new ServiceException("You cannot add yourself as a contact", HttpStatus.BAD_REQUEST);
            }

            if (contactExists(userId, existingUser.getId())) {
                throw new ServiceException("Contact already exists for this user", HttpStatus.BAD_REQUEST);
            }

            ContactDTO contactDTO = new ContactDTO();
            contactDTO.setUserId(userId);
            contactDTO.setContactUserId(existingUser.getId());
            contactDTO.setContactStatus(ContactStatus.ADDED);
            contactDTO.setEmailStatus(EmailStatus.NOT_APPLICABLE);
            contactDTO.setEmail(email);

            return saveContact(contactDTO);

        } catch (ServiceException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                return handleInviteFlow(userId, email);
            }
            throw ex;
        }
    }

    private ContactDTO handleInviteFlow(String userId, String email) {
        logger.info("Inviting unregistered email={} on behalf of userId={}", email, userId);

        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setUserId(userId);
        contactDTO.setContactUserId(null);
        contactDTO.setContactStatus(ContactStatus.INVITED);
        contactDTO.setEmailStatus(EmailStatus.PENDING);
        contactDTO.setEmail(email);
        ContactDTO savedContact = saveContact(contactDTO);

        CompletableFuture.runAsync(() -> {
            boolean sent = emailService.sendEmail(new EmailDTO(email, "You're invited to join ChatApp!", "Hi there!\n\nYou've been invited to join ChatApp. " + "Click here to register:\nhttps://yourapp.com/register"));
            updateEmailStatus(savedContact.getId(), sent ? EmailStatus.SENT : EmailStatus.FAILED);
        }, taskExecutor);

        return savedContact;
    }

    private boolean contactExists(String userId, String contactUserId) {
        return !contactRepository.findByUserIdAndContactUserId(userId, contactUserId).isEmpty();
    }

    private void updateEmailStatus(String contactId, EmailStatus status) {
        contactRepository.findById(contactId).ifPresent(contact -> {
            contact.setEmailStatus(status);
            contactRepository.save(contact);
        });
    }

    private ContactDTO saveContact(ContactDTO contactDTO) {
        try {
            Contact contactEntity = Mapper.mapToContactEntity(contactDTO);
            Contact saved = contactRepository.save(contactEntity);
            logger.info("Contact saved successfully for userId={}", contactDTO.getUserId());
            return Mapper.mapToContactDTO(saved);

        } catch (Exception ex) {
            logger.error("Failed to save contact for userId={}, reason={}", contactDTO.getUserId(), ex.getMessage(), ex);
            throw new ServiceException("Failed to save contact", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

