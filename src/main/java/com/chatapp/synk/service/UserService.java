package com.chatapp.synk.service;

import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.dto.UserStatusDTO;

import java.util.List;


public interface UserService {
    List<UserDTO> getAllUsers();

    UserDTO getUserByPhoneNumberOrEmail(String phoneNumberOrEmail);

    UserDTO getUserById(String userId);

    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(String userId, UserDTO userDTO);

    UserDTO forgotPassword(AuthDTO authDTO);

    void deleteUser(String userId);

    List<UserStatusDTO> getLastActiveUserStatus(String userId);
}
