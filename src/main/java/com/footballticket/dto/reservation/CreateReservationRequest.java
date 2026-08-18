package com.footballticket.dto.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(
    @NotNull @Positive Long userId,
    @NotNull @Positive Integer quantity) {

}
