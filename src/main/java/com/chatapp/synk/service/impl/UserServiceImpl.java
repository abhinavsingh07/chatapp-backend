package com.chatapp.synk.service.impl;

import com.chatapp.synk.security_validator.InputSecurityUtils;
import com.chatapp.synk.security_validator.InputValidationAndSanitizationService;
import com.chatapp.synk.security_validator.UserInputValidator;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.entity.Contact;
import com.chatapp.synk.entity.User;
import com.chatapp.synk.entity.UserRole;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.RoleName;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.repository.ContactRepository;
import com.chatapp.synk.repository.UserRepository;
import com.chatapp.synk.repository.UserRoleRepository;
import com.chatapp.synk.service.UserService;
import com.chatapp.synk.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContactRepository contactRepository;
    private final UserRoleRepository userRoleRepository;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, ContactRepository contactRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.contactRepository = contactRepository;
        this.userRoleRepository = userRoleRepository;
    }

    //id is userId
    @Override
    @Cacheable(value = "userListCache", key = "'allUsers'", unless = "#result == null or #result.isEmpty()")
    public List<UserDTO> getAllUsers() {
        logger.info("Fetching all users");
        List<UserDTO> allusers = userRepository.findAll().stream().map(Mapper::mapToUserDTO).collect(Collectors.toList());
        //remove password from all users
        allusers.forEach(user -> user.setPassword("********")); // Mask passwords
        return allusers;
    }

    @Override
    //using in userdetails service.
    //Caching we do explicitly using cache manage inside method
    public UserDTO getUserByPhoneNumberOrEmail(String phoneNumberOrEmail) {
        Optional<UserDTO> result;

        String validPhoneNumberOrEmail = InputSecurityUtils.secureLoginId(phoneNumberOrEmail);

        if (UserInputValidator.isValidEmail(validPhoneNumberOrEmail)) {
            logger.info("Fetching user by email: {}", validPhoneNumberOrEmail);
            result = userRepository.findByEmail(validPhoneNumberOrEmail).map(Mapper::mapToUserDTO);
        } else if (UserInputValidator.isValidPhoneNumber(validPhoneNumberOrEmail)) {
            logger.info("Fetching user by phone number: {}", validPhoneNumberOrEmail);
            result = userRepository.findByPhoneNumber(validPhoneNumberOrEmail).map(Mapper::mapToUserDTO);
        } else {
            logger.error("Invalid identifier format: {}", validPhoneNumberOrEmail);
            throw new ServiceException("Identifier must be a valid email or phone number", HttpStatus.BAD_REQUEST);
        }

        if (result.isEmpty()) {
            logger.warn("No user found with identifier: {}", validPhoneNumberOrEmail);
            throw new ServiceException("User not found with ID", HttpStatus.NOT_FOUND);
        }

        // Manually cache this using CacheManager (optional)
        //cacheManager.getCache("userCache").put(result.get().getId(), result.get());
        return result.get();
    }

    @Override
    @Cacheable(value = "userCache", key = "#userId", unless = "#result == null")
    public UserDTO getUserById(String userId) {
        logger.info("Fetching user by ID: {}", userId);
        String validId = InputSecurityUtils.secureId(userId);

        Optional<UserDTO> result = userRepository.findById(validId).map(Mapper::mapToUserDTO);

        if (result.isEmpty()) {
            logger.warn("No user found with ID: {}", validId);
            throw new ServiceException("User not found with ID", HttpStatus.NOT_FOUND);
        }

        UserDTO userDTO = result.get();
        userDTO.setPassword("********"); // Mask password for security
        return result.get();
    }


    @Override
    @Transactional//Now the entire flow, including saving user and updating contacts, happens in a single transaction.
    @Caching(put = {@CachePut(value = "userCache", key = "#result.id", unless = "#result == null")},
            evict = {@CacheEvict(value = "userListCache", key = "'allUsers'", beforeInvocation = true)})
    public UserDTO createUser(UserDTO userDTO) {
        logger.info("Creating new user with phone: {}", userDTO.getPhoneNumber());
        try {
            //first validate the dto
            UserDTO vaildatedDTO = InputValidationAndSanitizationService.validateAndSanitize(userDTO);
            User user = Mapper.mapToUserEntity(vaildatedDTO, passwordEncoder);
            // Always assign ROLE_USER by default
            UserRole userRole = userRoleRepository.findByName(RoleName.ROLE_USER).orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setUserRole(userRole.getName());

            User savedUser = userRepository.save(user);
            logger.info("User saved successfully with ID: {}", savedUser.getId());
            //handle invited flow if applicable
            handleInvitedFlow(savedUser);

            return Mapper.mapToUserDTO(savedUser);
        } catch (Exception ex) {
            logger.error("Unexpected error while creating user", ex);
            throw new ServiceException(ex.getMessage());
        }
    }


    private void handleInvitedFlow(User savedUser) {
        logger.info("Handling invited flow for user: {}", savedUser.getEmail());
        List<Contact> contacts = contactRepository.findByEmailAndContactUserIdIsNull(savedUser.getEmail());
        if (!contacts.isEmpty()) {
            // Update contacts whose email matches this new user’s email but have null contactUserId
            int updatedCount = contactRepository.updateContactUserIdByEmail(savedUser.getId(), ContactStatus.ADDED, savedUser.getEmail());
            logger.info("Updated contactUserId for {} contacts matching email  {}", updatedCount, savedUser.getEmail());
        } else {
            logger.info("No pending contacts to update for email {}", savedUser.getEmail());
        }
    }


    @Override
    @Caching(put = {@CachePut(value = "userCache", key = "#userId", unless = "#result == null")},
            evict = {@CacheEvict(value = "userListCache", key = "'allUsers'",beforeInvocation = true)})
    public UserDTO updateUser(String userId, UserDTO userDTO) {
        logger.info("Updating user with ID: {}", userId);
        String validId = InputSecurityUtils.secureId(userId);

        Optional<User> optionalUser = userRepository.findById(validId);
        if (optionalUser.isEmpty()) {
            logger.error("User not found while updating. ID: {}", validId);
            throw new ServiceException("User not found with ID", HttpStatus.NOT_FOUND);
        }
        UserDTO validDTO = InputValidationAndSanitizationService.validateAndSanitize(userDTO);
        try {
            User user = optionalUser.get();
            user.setName(validDTO.getName());
            user.setProfilePictureUrl(validDTO.getProfilePictureUrl());
            user.setAbout(validDTO.getAbout());

            User updatedUser = userRepository.save(user);
            logger.info("User updated successfully. ID: {}", updatedUser.getId());
            return Mapper.mapToUserDTO(updatedUser);
        } catch (Exception ex) {
            logger.error("Error while updating user with ID: {}", userId, ex.getMessage());
            throw new ServiceException(ex.getMessage());
        }
    }

    @Override
    @Caching(evict = {@CacheEvict(value = "userCache", key = "#userId", beforeInvocation = true),      // evict single user
            @CacheEvict(value = "userListCache", key = "'allUsers'", beforeInvocation = true) // evict cached user list beforeInvocation = true evict cache before even if method runs successfuly or not
    })
    public void deleteUser(String userId) {
        logger.info("Deleting user with ID: {}", userId);
        String validId = InputSecurityUtils.secureId(userId);
        userRepository.deleteById(validId);
        logger.info("User with ID {} deleted successfully", userId);
    }
}
