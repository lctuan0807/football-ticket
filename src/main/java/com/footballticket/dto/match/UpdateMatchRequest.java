package com.footballticket.dto.match;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record UpdateMatchRequest(
    @NotNull String competition,
    @NotNull String stage,
    @NotNull String season,
    @NotNull String homeTeam,
    @NotNull String awayTeam,
    @NotNull LocalDateTime kickoffAt,
    @NotNull String stadium) {

}
