package com.footballticket.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.footballticket.dto.auth.AuthResponse;
import com.footballticket.dto.auth.LoginRequest;
import com.footballticket.dto.auth.RegisterRequest;
import com.footballticket.dto.user.UserDTO;
import com.footballticket.entity.UserEntity;
import com.footballticket.exceptions.InvalidCredentialsException;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.repository.UserRepository;
import com.footballticket.security.JwtService;
import com.footballticket.service.AuthService;
import com.footballticket.service.TokenBlacklistService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final UserRepository userRepository;
  private final ModelMapper modelMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final TokenBlacklistService tokenBlacklistService;

  @Override
  public UserDTO register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new ResourceAlreadyExistsException("Username already taken!");
    }

    UserEntity user = new UserEntity();
    user.setUsername(request.username());
    user.setPassword(passwordEncoder.encode(request.password()));

    UserEntity saved = userRepository.save(user);
    log.info("User registered: {} (id={})", saved.getUsername(), saved.getId());

    return modelMapper.map(saved, UserDTO.class);
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    } catch (BadCredentialsException e) {
      throw new InvalidCredentialsException("Invalid username or password!");
    }

    String accessToken = jwtService.generateToken(request.username());
    log.info("User logged in: {}", request.username());

    return new AuthResponse(accessToken, "Bearer", jwtService.getExpirationMs() / 1000);
  }

  @Override
  public void logout(String token) {
    tokenBlacklistService.blacklist(token);
  }
}
