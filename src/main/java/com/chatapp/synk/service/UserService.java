package com.chatapp.synk.service;

import com.chatapp.synk.dto.UserDTO;

import java.util.List;


public interface UserService {
    UserDTO getUserById(String userId);

    UserDTO getUserByPhoneNumberOrEmail(String phoneNumberOrEmail);

    List<UserDTO> searchUsers(String phonePart, String emailPart);

    List<UserDTO> getAllUsers();

    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(String userId, UserDTO userDTO);

    void deleteUser(String userId);
}
