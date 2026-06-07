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

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        if (logger.isDebugEnabled()) {
            logger.debug("Registering new user with identifier: {}", maskIdentifier(userDTO.getPhoneNumber()));
        }

        try {
            UserDTO savedUser = createUser(userDTO);
            savedUser.setPassword("********"); // Mask password in response
            logger.info("User registration successful. User ID: {}", savedUser.getId());
            return savedUser;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
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
        if (userDTO == null) {
            throw new ServiceException("User update data is required", HttpStatus.BAD_REQUEST);
        }

        try {
            User user = optionalUser.get();

            if (userDTO.getName() != null) {
                user.setName(InputSecurityUtils.secureName(userDTO.getName()));
            }
            if (userDTO.getProfilePictureUrl() != null) {
                user.setProfilePictureUrl(userDTO.getProfilePictureUrl());//for now no check
            }
            if (userDTO.getAbout() != null) {
                user.setAbout(InputSecurityUtils.secureAbout(userDTO.getAbout()));
            }
            updatePasswordIfRequested(user, userDTO);

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

    @Override
    @Transactional
    public UserDTO forgotPassword(AuthDTO authDTO) {
        if (authDTO == null) {
            throw new ServiceException("Forgot password request data is required", HttpStatus.BAD_REQUEST);
        }

        String phoneNumberOrEmail = validateForgotPasswordIdentifier(authDTO.getPhoneNumberOrEmail());
        String newPassword = validateForgotPasswordPassword(authDTO.getPassword());

        if (!isStrongPassword(newPassword)) {
            throw new ServiceException("Password must be at least 8 characters and include uppercase, lowercase, and a digit", HttpStatus.BAD_REQUEST);
        }
         // TODO: Validate OTP before allowing password reset.

        // TODO: Reject password reset when OTP is missing, expired, or already used.

        UserDTO existingUser = getUserForForgotPassword(phoneNumberOrEmail);
        User user = userRepository.findById(existingUser.getId())
                .orElseThrow(() -> new ServiceException("User not found with ID", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(user);
        logger.info("Forgot password reset completed for user ID: {}", updatedUser.getId());

        UserDTO updatedUserDTO = Mapper.mapToUserDTO(updatedUser);
        updatedUserDTO.setPassword("********");
        return updatedUserDTO;
    }

    @Override
    public JwtResponse authenticate(AuthDTO authDTO) {
        if (logger.isDebugEnabled()) {
            logger.debug("Authentication request received for identifier: {}",
                    maskIdentifier(authDTO.getPhoneNumberOrEmail()));
        }

        AuthDTO sanitizedDTO = InputValidationAndSanitizationService.validateAndSanitize(authDTO);
        UserDTO user = authenticate(sanitizedDTO.getPhoneNumberOrEmail(), sanitizedDTO.getPassword());

        String role = user.getRoleName() != null ? user.getRoleName().name() : "";
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", role.isEmpty() ? List.of() : List.of(role));
        claims.put("id", user.getId());
        // dont store in jwt. In jwt only add important info like id, role etc.
        // even if user updates info we dont need to refresh jwt
        // user directly fetch latest details by id this is the main purpose to remove
        // other details
        // claims.put("email", user.getEmail());
        // claims.put("name", user.getName());//dont store in jwt

        String token = jwtUtil.generateAccessToken(claims, user.getPhoneNumber());
        String refreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());

        // Save refresh token for later validation
        // refreshTokenService.saveRefreshToken(refreshToken, user.getUsername());

        if (logger.isDebugEnabled()) {
            logger.debug("JWT token generated for user: {}", maskIdentifier(user.getPhoneNumber()));
        }

        return new JwtResponse(token, refreshToken, user.getEmail(), user.getName(),
                role, user.getEmail(), user.getProfilePictureUrl(), user.getId());
    }

    private UserDTO authenticate(String username, String password) throws ServiceException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Attempting authentication for user: {}", maskIdentifier(username));
            }

            UserDTO user = getUserByPhoneNumberOrEmail(username);
            if (!passwordEncoder.matches(password, user.getPassword())) {
                logger.warn("Authentication failed - invalid credentials for user: {}", maskIdentifier(username));
                throw new ServiceException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Authentication successful for user: {}", maskIdentifier(username));
            }
            return user;
        } catch (ServiceException e) {
            logger.warn("Authentication failed - invalid credentials for user: {}", maskIdentifier(username));
            throw new ServiceException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public JwtResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || isBlank(request.getRefreshToken())) {
            throw new ServiceException("Refresh token is required", HttpStatus.BAD_REQUEST);
        }

        String refreshToken = request.getRefreshToken();
        String username = jwtUtil.extractUsername(refreshToken);
        if (isBlank(username)) {
            throw new InvalidTokenException("Refresh token validation failed - username not found");
        }

        UserDTO user = getUserForRefreshToken(username);
        String role = user.getRoleName() != null ? user.getRoleName().name() : "";

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", role.isEmpty() ? List.of() : List.of(role));
        claims.put("id", user.getId());

        String newToken = jwtUtil.generateAccessToken(claims, user.getPhoneNumber());
        logger.info("New JWT token generated via refresh for user ID: {}", user.getId());

        return new JwtResponse(newToken, refreshToken, user.getEmail(), user.getName(),
                role, user.getEmail(), user.getProfilePictureUrl(), user.getId());
    }

    private UserDTO getUserForRefreshToken(String username) {
        try {
            return getUserByPhoneNumberOrEmail(username);
        } catch (ServiceException ex) {
            throw new InvalidTokenException("Refresh token validation failed - user not found", ex);
        }
    }

    private String validateForgotPasswordIdentifier(String phoneNumberOrEmail) {
        try {
            return InputSecurityUtils.secureLoginId(phoneNumberOrEmail);
        } catch (SecurityException ex) {
            throw new ServiceException("Phone number or email must be valid", HttpStatus.BAD_REQUEST);
        }
    }

    private String validateForgotPasswordPassword(String password) {
        try {
            return InputSecurityUtils.securePassword(password);
        } catch (SecurityException ex) {
            throw new ServiceException(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private UserDTO getUserForForgotPassword(String phoneNumberOrEmail) {
        try {
            return getUserByPhoneNumberOrEmail(phoneNumberOrEmail);
        } catch (ServiceException ex) {
            if (HttpStatus.NOT_FOUND.equals(ex.getStatus())) {
                throw new ServiceException("No account found for provided phone number or email", HttpStatus.NOT_FOUND);
            }
            throw ex;
        }
    }

    private void updatePasswordIfRequested(User user, UserDTO userDTO) {
        boolean passwordUpdateRequested = !isBlank(userDTO.getOldPassword())
                || !isBlank(userDTO.getNewPassword())
                || !isBlank(userDTO.getConfirmPassword());

        if (!passwordUpdateRequested) {
            return;
        }

        String oldPassword = InputSecurityUtils.securePassword(userDTO.getOldPassword());
        String newPassword = InputSecurityUtils.securePassword(userDTO.getNewPassword());
        String confirmPassword = InputSecurityUtils.securePassword(userDTO.getConfirmPassword());

        if (isBlank(oldPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            throw new ServiceException("Old password, new password, and confirm password are required", HttpStatus.BAD_REQUEST);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ServiceException("Old password is incorrect", HttpStatus.BAD_REQUEST);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ServiceException("New password and confirm password do not match", HttpStatus.BAD_REQUEST);
        }
        if (!isStrongPassword(newPassword)) {
            throw new ServiceException("New password must be at least 8 characters and include uppercase, lowercase, and a digit", HttpStatus.BAD_REQUEST);
        }
        if (oldPassword.equals(newPassword)) {
            throw new ServiceException("New password must be different from old password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isStrongPassword(String password) {
        return password != null
                && password.length() >= 8
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit);
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null)
            return "null";
        // mask email/phone for logs: e.g., 98****1234 or j***@domain.com
        if (identifier.contains("@")) {
            int idx = identifier.indexOf("@");
            return identifier.charAt(0) + "***" + identifier.substring(idx);
        } else if (identifier.length() > 4) {
            return identifier.substring(0, 2) + "****" + identifier.substring(identifier.length() - 2);
        }
        return "****";
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

    // This method returns lastactive timestamp for multiple users, as user can be active in multiple devices, so we will return the list of status of all devices
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

