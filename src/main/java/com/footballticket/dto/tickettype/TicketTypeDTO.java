package com.footballticket.dto.tickettype;

import lombok.Data;

@Data
public class TicketTypeDTO {
  private Long id;
  private Long matchId;
  private String name;
  private String description;
  private Integer price;
  private Integer quantity;
  private Integer availableQuantity;
}
