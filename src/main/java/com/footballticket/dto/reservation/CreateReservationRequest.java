package com.footballticket.dto.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(
    @NotNull @Positive Integer quantity) {

}
