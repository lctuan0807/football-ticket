package com.footballticket.dto.user;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserDTO {
  private Long id;
  private String username;
  private String role;
  private LocalDateTime createdAt;
}
