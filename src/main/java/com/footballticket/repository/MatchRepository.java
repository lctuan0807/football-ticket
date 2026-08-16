package com.footballticket.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballticket.entity.MatchEntity;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

  boolean existsByHomeTeamAndAwayTeamAndSeason(String homeTeam, String awayTeam, String season);
}
