package com.chatapp.synk.service.impl;

import com.chatapp.synk.chat.redis.RedisSessionStore;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.dto.UserStatusDTO;
import com.chatapp.synk.entity.Contact;
import com.chatapp.synk.entity.User;
import com.chatapp.synk.entity.UserRole;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.RoleName;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.repository.ContactRepository;
import com.chatapp.synk.repository.UserRepository;
import com.chatapp.synk.repository.UserRoleRepository;
import com.chatapp.synk.security_validator.InputSecurityUtils;
import com.chatapp.synk.security_validator.InputValidationAndSanitizationService;
import com.chatapp.synk.security_validator.UserInputValidator;
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

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContactRepository contactRepository;
    private final UserRoleRepository userRoleRepository;
    private final RedisSessionStore redisSessionStore;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ContactRepository contactRepository,
                           UserRoleRepository userRoleRepository,
                           RedisSessionStore redisSessionStore) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.contactRepository = contactRepository;
        this.userRoleRepository = userRoleRepository;
        this.redisSessionStore = redisSessionStore;
    }

    @Override
    @Cacheable(value = "userListCache", key = "'allUsers'", unless = "#result == null or #result.isEmpty()")
    public List<UserDTO> getAllUsers() {
        logger.debug("Fetching all users");
        List<UserDTO> allUsers = userRepository.findAll()
                .stream()
                .map(Mapper::mapToUserDTO)
                .collect(Collectors.toList());
        allUsers.forEach(user -> user.setPassword("********")); // Mask passwords
        return allUsers;
    }

    @Override
    public UserDTO getUserByPhoneNumberOrEmail(String phoneNumberOrEmail) {
        Optional<UserDTO> result;
        String validPhoneNumberOrEmail = InputSecurityUtils.secureLoginId(phoneNumberOrEmail);

        if (UserInputValidator.isValidEmail(validPhoneNumberOrEmail)) {
            logger.debug("Fetching user by email: {}", validPhoneNumberOrEmail);
            result = userRepository.findByEmail(validPhoneNumberOrEmail).map(Mapper::mapToUserDTO);
        } else if (UserInputValidator.isValidPhoneNumber(validPhoneNumberOrEmail)) {
            logger.debug("Fetching user by phone number: {}", validPhoneNumberOrEmail);
            result = userRepository.findByPhoneNumber(validPhoneNumberOrEmail).map(Mapper::mapToUserDTO);
        } else {
            logger.error("Invalid identifier format: {}", validPhoneNumberOrEmail);
            throw new ServiceException("Identifier must be a valid email or phone number", HttpStatus.BAD_REQUEST);
        }

        if (result.isEmpty()) {
            logger.warn("No user found with identifier: {}", validPhoneNumberOrEmail);
            throw new ServiceException("User not found with ID", HttpStatus.NOT_FOUND);
        }

        return result.get();
    }

    @Override
    @Cacheable(value = "userCache", key = "#userId", unless = "#result == null")
    public UserDTO getUserById(String userId) {
        logger.debug("Fetching user by ID: {}", userId);
        String validId = InputSecurityUtils.secureId(userId);

        Optional<UserDTO> result = userRepository.findById(validId).map(Mapper::mapToUserDTO);

        if (result.isEmpty()) {
            logger.warn("No user found with ID: {}", validId);
            throw new ServiceException("User not found with ID", HttpStatus.NOT_FOUND);
        }

        UserDTO userDTO = result.get();
        userDTO.setPassword("********"); // Mask password
        return userDTO;
    }

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(value = "userCache", key = "#result.id", unless = "#result == null")
    }, evict = {
            @CacheEvict(value = "userListCache", key = "'allUsers'", beforeInvocation = true)
    })
    public UserDTO createUser(UserDTO userDTO) {
        logger.info("Creating new user with phone: {}", userDTO.getPhoneNumber());
        try {
            UserDTO validatedDTO = InputValidationAndSanitizationService.validateAndSanitize(userDTO);
            User user = Mapper.mapToUserEntity(validatedDTO, passwordEncoder);

            // Assign ROLE_USER
            UserRole userRole = userRoleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setUserRole(userRole.getName());

            User savedUser = userRepository.save(user);
            logger.info("User created successfully with ID: {}", savedUser.getId());

            handleInvitedFlow(savedUser);

            return Mapper.mapToUserDTO(savedUser);
        } catch (Exception ex) {
            logger.error("Unexpected error while creating user", ex);
            throw new ServiceException(ex.getMessage());
        }
    }

    private void handleInvitedFlow(User savedUser) {
        logger.debug("Handling invited flow for user: {}", savedUser.getEmail());
        List<Contact> contacts = contactRepository.findByEmailAndContactUserIdIsNull(savedUser.getEmail());
        if (!contacts.isEmpty()) {
            int updatedCount = contactRepository.updateContactUserIdByEmail(
                    savedUser.getId(),
                    ContactStatus.ADDED,
                    savedUser.getEmail()
            );
            logger.info("Updated contactUserId for {} contacts matching email {}", updatedCount, savedUser.getEmail());
        }
    }

    @Override
    @Caching(put = {
            @CachePut(value = "userCache", key = "#userId", unless = "#result == null")
    }, evict = {
            @CacheEvict(value = "userListCache", key = "'allUsers'", beforeInvocation = true)
    })
    public UserDTO updateUser(String userId, UserDTO userDTO) {
        logger.info("Updating user with ID: {}", userId);
        String validId = InputSecurityUtils.secureId(userId);

        Optional<User> optionalUser = userRepository.findById(validId);
        if (optionalUser.isEmpty()) {
            logger.warn("User not found while updating. ID: {}", validId);
            throw new ServiceException("User not found with ID", HttpStatus.NOT_FOUND);
        }

        try {
            UserDTO validDTO = InputValidationAndSanitizationService.validateAndSanitize(userDTO);
            User user = optionalUser.get();
            user.setName(validDTO.getName());
            user.setProfilePictureUrl(validDTO.getProfilePictureUrl());
            user.setAbout(validDTO.getAbout());

            User updatedUser = userRepository.save(user);
            logger.info("User updated successfully. ID: {}", updatedUser.getId());
            return Mapper.mapToUserDTO(updatedUser);
        } catch (Exception ex) {
            logger.error("Error while updating user with ID: {}", userId, ex);
            throw new ServiceException(ex.getMessage());
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#userId", beforeInvocation = true),
            @CacheEvict(value = "userListCache", key = "'allUsers'", beforeInvocation = true)
    })
    public void deleteUser(String userId) {
        logger.info("Deleting user with ID: {}", userId);
        String validId = InputSecurityUtils.secureId(userId);
        userRepository.deleteById(validId);
        logger.info("User deleted successfully. ID: {}", userId);
    }

    @Override
    public List<UserStatusDTO> getLastActiveUserStatus(String userId) {
        logger.debug("Fetching last active timestamp for user(s): {}", userId);
        long now = Instant.now().toEpochMilli();
        List<UserStatusDTO> result = new ArrayList<>();

        String[] userIds = Arrays.stream(userId.split(","))
                .map(InputSecurityUtils::secureId)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        for (String uid : userIds) {
            String lastActive = redisSessionStore.getLastActiveTimeStampUser(uid);
            if (lastActive != null) {
                boolean online = (now - Long.parseLong(lastActive)) <= 4000;// 4 seconds if user is offline
                result.add(new UserStatusDTO(uid, online, lastActive));
            }
        }
        return result;
    }
}

