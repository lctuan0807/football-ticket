package com.footballticket.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballticket.common.ApiResponse;
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
  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserDTO>> register(@RequestBody @Valid RegisterRequest request) {
    UserDTO user = authService.register(request);
    return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
  }
}
