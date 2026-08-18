package com.footballticket.dto.tickettype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTicketTypeRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive Integer price,
        @NotNull @Positive Integer quantity) {
}
