package com.footballticket.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.footballticket.dto.auth.AuthResponse;
import com.footballticket.dto.auth.LoginRequest;
import com.footballticket.dto.auth.RegisterRequest;
import com.footballticket.dto.user.UserDTO;
import com.footballticket.exceptions.InvalidCredentialsException;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.service.AuthService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthService authService;

  @Test
  void register_returnsCreatedUser_whenRequestIsValid() throws Exception {
    RegisterRequest request = new RegisterRequest("tuanle", "password123");

    UserDTO response = new UserDTO();
    response.setId(1L);
    response.setUsername(request.username());

    given(authService.register(any(RegisterRequest.class))).willReturn(response);

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code", is(200)))
        .andExpect(jsonPath("$.data.id", is(1)))
        .andExpect(jsonPath("$.data.username", is("tuanle")));
  }

  @Test
  void register_returnsBadRequest_whenPasswordIsTooShort() throws Exception {
    String requestJson = """
        {
          "username": "tuanle",
          "password": "short"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("Password must be at least 8 characters")));
  }

  @Test
  void register_returnsBadRequest_whenUsernameIsMissing() throws Exception {
    String requestJson = """
        {
          "password": "password123"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_returnsConflict_whenUsernameAlreadyExists() throws Exception {
    RegisterRequest request = new RegisterRequest("tuanle", "password123");

    given(authService.register(any(RegisterRequest.class)))
        .willThrow(new ResourceAlreadyExistsException("Username already taken!"));

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is(409)))
        .andExpect(jsonPath("$.message", is("Username already taken!")));
  }

  @Test
  void login_returnsAccessToken_whenCredentialsAreValid() throws Exception {
    LoginRequest request = new LoginRequest("tuanle", "password123");
    AuthResponse response = new AuthResponse("access-token", "Bearer", 3600L);

    given(authService.login(any(LoginRequest.class))).willReturn(response);

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken", is("access-token")))
        .andExpect(jsonPath("$.data.tokenType", is("Bearer")));
  }

  @Test
  void login_returnsUnauthorized_whenCredentialsAreInvalid() throws Exception {
    LoginRequest request = new LoginRequest("tuanle", "wrongpassword");

    given(authService.login(any(LoginRequest.class)))
        .willThrow(new InvalidCredentialsException("Invalid username or password!"));

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message", is("Invalid username or password!")));
  }

  @Test
  void logout_returnsNoContent_whenAuthorizationHeaderIsPresent() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout")
        .header(HttpHeaders.AUTHORIZATION, "Bearer some-token"))
        .andExpect(status().isNoContent());

    verify(authService).logout(eq("some-token"));
  }

  @Test
  void logout_returnsNoContent_whenAuthorizationHeaderIsMissing() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout"))
        .andExpect(status().isNoContent());

    verify(authService, never()).logout(any());
  }
}
