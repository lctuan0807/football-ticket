package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.footballticket.dto.auth.AuthResponse;
import com.footballticket.dto.auth.LoginRequest;
import com.footballticket.dto.auth.RegisterRequest;
import com.footballticket.dto.user.UserDTO;
import com.footballticket.entity.UserEntity;
import com.footballticket.exceptions.InvalidCredentialsException;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.repository.UserRepository;
import com.footballticket.security.JwtService;
import com.footballticket.service.RedisService;
import com.footballticket.service.TokenBlacklistService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ModelMapper modelMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtService jwtService;

  @Mock
  private RedisService redisService;

  private AuthServiceImpl authService;

  private final RegisterRequest registerRequest = new RegisterRequest("tuanle", "password123");
  private final LoginRequest loginRequest = new LoginRequest("tuanle", "password123");

  @BeforeEach
  void setUp() {
    TokenBlacklistService tokenBlacklistService = new TokenBlacklistService(redisService, jwtService);
    authService = new AuthServiceImpl(userRepository, modelMapper, passwordEncoder, authenticationManager,
        jwtService, tokenBlacklistService);
  }

  @Test
  void register_savesUserAndReturnsDto_whenUsernameDoesNotExist() {
    given(userRepository.existsByUsername(registerRequest.username())).willReturn(false);
    given(passwordEncoder.encode(registerRequest.password())).willReturn("hashed-password");

    UserEntity saved = new UserEntity();
    saved.setId(1L);
    saved.setUsername(registerRequest.username());
    saved.setPassword("hashed-password");
    given(userRepository.save(any(UserEntity.class))).willReturn(saved);

    UserDTO expectedDto = new UserDTO();
    expectedDto.setId(1L);
    expectedDto.setUsername(registerRequest.username());
    given(modelMapper.map(saved, UserDTO.class)).willReturn(expectedDto);

    UserDTO result = authService.register(registerRequest);

    assertThat(result).isSameAs(expectedDto);

    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(captor.capture());
    UserEntity persisted = captor.getValue();
    assertThat(persisted.getUsername()).isEqualTo(registerRequest.username());
    assertThat(persisted.getPassword()).isEqualTo("hashed-password");
  }

  @Test
  void register_throwsResourceAlreadyExistsException_whenUsernameAlreadyExists() {
    given(userRepository.existsByUsername(registerRequest.username())).willReturn(true);

    assertThatThrownBy(() -> authService.register(registerRequest))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("Username already taken!");

    verify(userRepository, never()).save(any(UserEntity.class));
  }

  @Test
  void login_returnsAuthResponse_whenCredentialsAreValid() {
    given(jwtService.generateToken(loginRequest.username())).willReturn("access-token");
    given(jwtService.getExpirationMs()).willReturn(3600000L);

    AuthResponse result = authService.login(loginRequest);

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.tokenType()).isEqualTo("Bearer");
    assertThat(result.expiresIn()).isEqualTo(3600L);
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void login_throwsInvalidCredentialsException_whenCredentialsAreInvalid() {
    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willThrow(new BadCredentialsException("Bad credentials"));

    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid username or password!");
  }

  @Test
  void logout_blacklistsTokenForRemainingTtl() {
    String token = "some-token";
    Date expiration = new Date(System.currentTimeMillis() + 60_000);
    given(jwtService.extractExpiration(token)).willReturn(expiration);

    authService.logout(token);

    verify(redisService).setObject(eq("blacklist:token:" + token), eq(true), any(Duration.class));
  }
}
