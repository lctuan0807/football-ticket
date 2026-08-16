package com.footballticket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.footballticket.entity.MatchEntity;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

  boolean existsByHomeTeamAndAwayTeamAndSeason(String homeTeam, String awayTeam, String season);

  Page<MatchEntity> findByStatus(Integer status, Pageable pageable);
}
