package com.footballticket.dto.match;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMatchRequest(
        @NotBlank(message = "Competition is required") String competition,
        @NotBlank(message = "Stage is required") String stage,
        @NotBlank(message = "Season is required") String season,
        @NotBlank(message = "Home team is required") String homeTeam,
        @NotBlank(message = "Away team is required") String awayTeam,
        @NotNull(message = "Kickoff time is required") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime kickoffAt,
        @NotBlank(message = "Stadium is required") String stadium) {
}
