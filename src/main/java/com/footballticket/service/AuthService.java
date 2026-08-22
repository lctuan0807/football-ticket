package com.footballticket.service;

import com.footballticket.dto.auth.AuthResponse;
import com.footballticket.dto.auth.LoginRequest;
import com.footballticket.dto.auth.RegisterRequest;
import com.footballticket.dto.user.UserDTO;

public interface AuthService {
  UserDTO register(RegisterRequest request);

  AuthResponse login(LoginRequest request);

  AuthResponse refresh(String refreshToken);

  void logout(String accessToken, String refreshToken);
}
