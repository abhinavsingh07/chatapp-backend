package com.chatapp.synk.service;

import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.dto.UserStatusDTO;

import java.util.List;


public interface UserService {
    List<UserDTO> getAllUsers();

    UserDTO getUserByPhoneNumberOrEmail(String phoneNumberOrEmail);

    UserDTO getUserById(String userId);

    UserDTO registerUser(UserDTO userDTO);

    UserDTO updateUser(String userId, UserDTO userDTO);

    UserDTO updateLastSeen(String userId);

    void deleteUser(String userId);

    List<UserStatusDTO> getLastActiveUserStatus(String userId);
}
