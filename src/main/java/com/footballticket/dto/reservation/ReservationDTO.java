package com.footballticket.dto.reservation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationDTO {
  private Long id;
  private Long ticketTypeId;
  private Long matchId;
  private Integer quantity;
  private String status;
  private LocalDateTime expiresAt;
  private LocalDateTime createdAt;
}
