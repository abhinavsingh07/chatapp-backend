package com.chatapp.synk.service;

import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.RefreshTokenRequest;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.security.JwtResponse;

public interface AuthService {

    UserDTO forgotPassword(AuthDTO authDTO);

    JwtResponse authenticate(AuthDTO authDTO);

    JwtResponse refreshToken(RefreshTokenRequest request);
}
