package com.footballticket.dto.tickettype;

public record TicketTypeDTO(
    Long id,
    Long matchId,
    String name,
    Integer price,
    Integer quantity,
    Integer availableQuantity) {
}
