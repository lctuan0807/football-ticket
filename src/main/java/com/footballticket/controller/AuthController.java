package com.footballticket.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballticket.common.ApiResponse;
import com.footballticket.dto.auth.AuthResponse;
import com.footballticket.dto.auth.LoginRequest;
import com.footballticket.dto.auth.RefreshTokenRequest;
import com.footballticket.dto.auth.RegisterRequest;
import com.footballticket.dto.user.UserDTO;
import com.footballticket.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private static final String BEARER_PREFIX = "Bearer ";

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserDTO>> register(@RequestBody @Valid RegisterRequest request) {
    UserDTO user = authService.register(request);
    return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
    AuthResponse authResponse = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
    AuthResponse authResponse = authService.refresh(request.refreshToken());
    return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody(required = false) RefreshTokenRequest request) {
    String accessToken = authHeader != null && authHeader.startsWith(BEARER_PREFIX)
        ? authHeader.substring(BEARER_PREFIX.length())
        : null;
    String refreshToken = request != null ? request.refreshToken() : null;
    authService.logout(accessToken, refreshToken);
    return ResponseEntity.noContent().build();
  }
}
