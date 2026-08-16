package com.footballticket.dto.match;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MatchDTO {
  private Long id;
  private String competition;
  private String stage;
  private String season;
  private String homeTeam;
  private String awayTeam;
  private String date;
  private String status;
  private LocalDateTime kickoffTime;
}
