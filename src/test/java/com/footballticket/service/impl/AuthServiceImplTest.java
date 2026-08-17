package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.footballticket.dto.auth.RegisterRequest;
import com.footballticket.dto.user.UserDTO;
import com.footballticket.entity.UserEntity;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ModelMapper modelMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  private AuthServiceImpl authService;

  private final RegisterRequest request = new RegisterRequest("tuanle", "password123");

  @BeforeEach
  void setUp() {
    authService = new AuthServiceImpl(userRepository, modelMapper, passwordEncoder);
  }

  @Test
  void register_savesUserAndReturnsDto_whenUsernameDoesNotExist() {
    given(userRepository.existsByUsername(request.username())).willReturn(false);
    given(passwordEncoder.encode(request.password())).willReturn("hashed-password");

    UserEntity saved = new UserEntity();
    saved.setId(1L);
    saved.setUsername(request.username());
    saved.setPassword("hashed-password");
    given(userRepository.save(any(UserEntity.class))).willReturn(saved);

    UserDTO expectedDto = new UserDTO();
    expectedDto.setId(1L);
    expectedDto.setUsername(request.username());
    given(modelMapper.map(saved, UserDTO.class)).willReturn(expectedDto);

    UserDTO result = authService.register(request);

    assertThat(result).isSameAs(expectedDto);

    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(captor.capture());
    UserEntity persisted = captor.getValue();
    assertThat(persisted.getUsername()).isEqualTo(request.username());
    assertThat(persisted.getPassword()).isEqualTo("hashed-password");
  }

  @Test
  void register_throwsResourceAlreadyExistsException_whenUsernameAlreadyExists() {
    given(userRepository.existsByUsername(request.username())).willReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("Username already taken!");

    verify(userRepository, never()).save(any(UserEntity.class));
  }
}
