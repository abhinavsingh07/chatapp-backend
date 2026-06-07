package com.chatapp.synk.service;

import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.RefreshTokenRequest;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.dto.UserStatusDTO;
import com.chatapp.synk.security.JwtResponse;

import java.util.List;


public interface UserService {
    List<UserDTO> getAllUsers();

    UserDTO getUserByPhoneNumberOrEmail(String phoneNumberOrEmail);

    UserDTO getUserById(String userId);

    UserDTO createUser(UserDTO userDTO);

    UserDTO registerUser(UserDTO userDTO);

    UserDTO updateUser(String userId, UserDTO userDTO);

    UserDTO forgotPassword(AuthDTO authDTO);

    JwtResponse authenticate(AuthDTO authDTO);

    JwtResponse refreshToken(RefreshTokenRequest request);

    void deleteUser(String userId);

    List<UserStatusDTO> getLastActiveUserStatus(String userId);
}
