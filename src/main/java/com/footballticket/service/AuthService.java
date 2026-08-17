package com.footballticket.service;

import com.footballticket.dto.auth.RegisterRequest;
import com.footballticket.dto.user.UserDTO;

public interface AuthService {
  UserDTO register(RegisterRequest request);
}
