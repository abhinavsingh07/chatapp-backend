package com.chatapp.synk.service.impl;

import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.entity.Contact;
import com.chatapp.synk.entity.User;
import com.chatapp.synk.entity.UserRole;
import com.chatapp.synk.enums.RoleName;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.repository.ContactRepository;
import com.chatapp.synk.repository.UserRepository;
import com.chatapp.synk.repository.UserRoleRepository;
import com.chatapp.synk.service.UserService;
import com.chatapp.synk.util.AppUtils;
import com.chatapp.synk.util.Mapper;
import com.chatapp.synk.util.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private ContactRepository contactRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;

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
        if (StringUtility.isEmpty(phoneNumberOrEmail)) {
            logger.error("Input identifier is empty. Cannot fetch user.");
            throw new ServiceException("Phone number or email must be provided", HttpStatus.BAD_REQUEST);
        }

        String trimmed = phoneNumberOrEmail.trim();

        Optional<UserDTO> result;

        if (AppUtils.isValidEmail(trimmed)) {
            logger.info("Fetching user by email: {}", trimmed);
            result = userRepository.findByEmail(trimmed).map(Mapper::mapToUserDTO);
        } else if (AppUtils.isValidPhoneNumber(trimmed)) {
            logger.info("Fetching user by phone number: {}", trimmed);
            result = userRepository.findByPhoneNumber(trimmed).map(Mapper::mapToUserDTO);
        } else {
            logger.error("Invalid identifier format: {}", trimmed);
            throw new ServiceException("Identifier must be a valid email or phone number", HttpStatus.BAD_REQUEST);
        }

        if (result.isEmpty()) {
            logger.warn("No user found with identifier: {}", trimmed);
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
        Optional<UserDTO> result = userRepository.findById(userId).map(Mapper::mapToUserDTO);

        if (result.isEmpty()) {
            logger.warn("No user found with ID: {}", userId);
            throw new ServiceException("User not found with ID", HttpStatus.NOT_FOUND);
        }

        UserDTO userDTO= result.get();
        userDTO.setPassword("********"); // Mask password for security
        return result.get();
    }


    @Override
    @Transactional//Now the entire flow, including saving user and updating contacts, happens in a single transaction.
    @CachePut(value = "userCache", key = "#result.id", unless = "#result == null")
    public UserDTO createUser(UserDTO userDTO) {
        logger.info("Creating new user with phone: {}", userDTO.getPhoneNumber());
        try {
            User user = Mapper.mapToUserEntity(userDTO, passwordEncoder);
            // Always assign ROLE_USER by default
            UserRole userRole = userRoleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setUserRole(userRole.getName());

            User savedUser = userRepository.save(user);
            logger.info("User saved successfully with ID: {}", savedUser.getId());
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
            int updatedCount = contactRepository.updateContactUserIdByEmail(savedUser.getId(), savedUser.getEmail());
            logger.info("Updated contactUserId for {} contacts matching email  {}", updatedCount, savedUser.getEmail());
        } else {
            logger.info("No pending contacts to update for email {}", savedUser.getEmail());
        }
    }


    @Override
    @CachePut(value = "userCache", key = "#userId", unless = "#result == null")
    public UserDTO updateUser(String userId, UserDTO userDTO) {
        logger.info("Updating user with ID: {}", userId);

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            logger.error("User not found while updating. ID: {}", userId);
            throw new ServiceException("User not found with ID", HttpStatus.NOT_FOUND);
        }

        try {
            User user = optionalUser.get();
            user.setName(userDTO.getName());
            user.setProfilePictureUrl(userDTO.getProfilePictureUrl());
            user.setAbout(userDTO.getAbout());

            User updatedUser = userRepository.save(user);
            logger.info("User updated successfully. ID: {}", updatedUser.getId());
            return Mapper.mapToUserDTO(updatedUser);
        } catch (Exception ex) {
            logger.error("Error while updating user with ID: {}", userId, ex.getMessage());
            throw new ServiceException(ex.getMessage());
        }
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public void deleteUser(String userId) {
        logger.info("Deleting user with ID: {}", userId);
        userRepository.deleteById(userId);
        logger.info("User with ID {} deleted successfully", userId);
    }
}
