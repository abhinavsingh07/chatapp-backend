package com.chatapp.synk.service.impl;

import com.chatapp.synk.chat.redis.RedisSessionStore;
import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.RefreshTokenRequest;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.dto.UserStatusDTO;
import com.chatapp.synk.entity.Contact;
import com.chatapp.synk.entity.User;
import com.chatapp.synk.entity.UserRole;
import com.chatapp.synk.enums.ContactStatus;
import com.chatapp.synk.enums.RoleName;
import com.chatapp.synk.exceptionHandler.InvalidTokenException;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.repository.ContactRepository;
import com.chatapp.synk.repository.UserRepository;
import com.chatapp.synk.repository.UserRoleRepository;
import com.chatapp.synk.security.JwtResponse;
import com.chatapp.synk.security.JwtUtil;
import com.chatapp.synk.security_validator.InputSecurityUtils;
import com.chatapp.synk.security_validator.InputValidationAndSanitizationService;
import com.chatapp.synk.security_validator.UserInputValidator;
import com.chatapp.synk.service.UserService;
import com.chatapp.synk.util.Mapper;
import com.chatapp.synk.util.PasswordUtil;
import com.chatapp.synk.util.StringUtil;

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
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ContactRepository contactRepository,
            UserRoleRepository userRoleRepository,
            RedisSessionStore redisSessionStore,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.contactRepository = contactRepository;
        this.userRoleRepository = userRoleRepository;
        this.redisSessionStore = redisSessionStore;
        this.jwtUtil = jwtUtil;
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
    public UserDTO registerUser(UserDTO userDTO) {
        if (logger.isDebugEnabled()) {
            logger.debug("Registering new user with identifier: {}", maskIdentifier(userDTO.getPhoneNumber()));
        }
        try {
            UserDTO validatedDTO = InputValidationAndSanitizationService.validateAndSanitize(userDTO);
            User user = Mapper.mapToUserEntity(validatedDTO, passwordEncoder);
            // Assign ROLE_USER
            UserRole userRole = userRoleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setUserRole(userRole.getName());

            // db call
            User savedUser = userRepository.save(user);
            // handle invited flow update status
            handleInvitedFlow(savedUser);

            // convert result to dto back
            UserDTO userdto = Mapper.mapToUserDTO(savedUser);
            userdto.setPassword("********"); // Mask password in response

            logger.info("User registration/creation successful. User ID: {}", savedUser.getId());
            return userdto;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            // @transactional rollback happens on runtime exception our ServiceException is
            // runtimeexception so it will work
            logger.error("Unexpected error during user creation", ex);
            throw new ServiceException("User creation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void handleInvitedFlow(User savedUser) {
        logger.debug("Handling invited flow for user: {}", savedUser.getEmail());
        List<Contact> contacts = contactRepository.findByEmailAndContactUserIdIsNull(savedUser.getEmail());
        if (!contacts.isEmpty()) {
            int updatedCount = contactRepository.updateContactUserIdByEmail(
                    savedUser.getId(),
                    ContactStatus.ADDED,
                    savedUser.getEmail());
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
        if (userDTO == null) {
            throw new ServiceException("User update data is required", HttpStatus.BAD_REQUEST);
        }

        try {
            User user = optionalUser.get();

            if (userDTO.getName() != null) {
                user.setName(InputSecurityUtils.secureName(userDTO.getName()));
            }
            if (userDTO.getProfilePictureUrl() != null) {
                user.setProfilePictureUrl(userDTO.getProfilePictureUrl());// for now no check
            }
            if (userDTO.getAbout() != null) {
                user.setAbout(InputSecurityUtils.secureAbout(userDTO.getAbout()));
            }
            // update password flow
            updatePasswordIfRequested(user, userDTO);
            // save to db
            User updatedUser = userRepository.save(user);
            logger.info("User updated successfully. ID: {}", updatedUser.getId());
            UserDTO updatedUserDTO = Mapper.mapToUserDTO(updatedUser);
            updatedUserDTO.setPassword("********");
            return updatedUserDTO;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error while updating user with ID: {}", userId, ex);
            throw new ServiceException(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void updatePasswordIfRequested(User user, UserDTO userDTO) {
        boolean passwordUpdateRequested = !StringUtil.isBlank(userDTO.getOldPassword())
                || !StringUtil.isBlank(userDTO.getNewPassword())
                || !StringUtil.isBlank(userDTO.getConfirmPassword());

        if (!passwordUpdateRequested) {
            return;
        }

        String oldPassword = InputSecurityUtils.securePassword(userDTO.getOldPassword());
        String newPassword = InputSecurityUtils.securePassword(userDTO.getNewPassword());
        String confirmPassword = InputSecurityUtils.securePassword(userDTO.getConfirmPassword());

        if (StringUtil.isBlank(oldPassword) || StringUtil.isBlank(newPassword) || StringUtil.isBlank(confirmPassword)) {
            throw new ServiceException("Old password, new password, and confirm password are required",
                    HttpStatus.BAD_REQUEST);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ServiceException("Old password is incorrect", HttpStatus.BAD_REQUEST);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ServiceException("New password and confirm password do not match", HttpStatus.BAD_REQUEST);
        }
        if (!PasswordUtil.isStrongPassword(newPassword)) {
            throw new ServiceException(
                    "New password must be at least 8 characters and include uppercase, lowercase, and a digit",
                    HttpStatus.BAD_REQUEST);
        }
        if (oldPassword.equals(newPassword)) {
            throw new ServiceException("New password must be different from old password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
    }

 

    @Override
    @Caching(put = {
            @CachePut(value = "userCache", key = "#userId", unless = "#result == null")
    }, evict = {
            @CacheEvict(value = "userListCache", key = "'allUsers'", beforeInvocation = true)
    })
    public UserDTO updateLastSeen(String userId) {
        logger.debug("Updating last seen for user ID: {}", userId);
        String validId = InputSecurityUtils.secureId(userId);
        // fetch user from db first
        User user = userRepository.findById(validId)
                .orElseThrow(() -> new ServiceException("User not found with ID", HttpStatus.NOT_FOUND));

        // find lastactive time from redis
        String lastActive = redisSessionStore.getLastActiveTimeStampUser(validId);
        if (StringUtil.isBlank(lastActive)) {
            throw new ServiceException("Last active timestamp not found", HttpStatus.NOT_FOUND);
        }
        // update field
        user.setUserlastSeen(parseLastActiveInstant(lastActive));
        // save to db
        User updatedUser = userRepository.save(user);
        return Mapper.mapToUserDTO(updatedUser);
    }

    private Instant parseLastActiveInstant(String lastActive) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(lastActive));
        } catch (NumberFormatException ex) {
            throw new ServiceException("Invalid last active timestamp", HttpStatus.BAD_REQUEST);
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

    // This method returns lastactive timestamp for multiple users, as user can be
    // active in multiple devices, so we will return the list of status of all
    // devices
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
