package com.footballticket.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.footballticket.dto.auth.RegisterRequest;
import com.footballticket.dto.user.UserDTO;
import com.footballticket.entity.UserEntity;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.repository.UserRepository;
import com.footballticket.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final UserRepository userRepository;
  private final ModelMapper modelMapper;
  private final PasswordEncoder passwordEncoder;

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
}
