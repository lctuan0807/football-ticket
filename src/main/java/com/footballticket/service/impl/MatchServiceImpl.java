package com.footballticket.service.impl;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.entity.MatchEntity;
import com.footballticket.enums.MatchStatusEnum;
import com.footballticket.exceptions.MatchAlreadyExistsException;
import com.footballticket.repository.MatchRepository;
import com.footballticket.service.MatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
  private final MatchRepository matchRepository;
  private final ModelMapper modelMapper;

  @Override
  public MatchDTO createMatch(CreateMatchRequest request) {
    MatchEntity match = new MatchEntity();

    match.setCompetition(request.competition());
    match.setStage(request.stage());
    match.setSeason(request.season());
    match.setHomeTeam(request.homeTeam());
    match.setAwayTeam(request.awayTeam());
    match.setKickoffAt(request.kickoffAt());
    match.setStadium(request.stadium());
    match.setStatus(MatchStatusEnum.SCHEDULED.toInt());
    match.setCreatedAt(LocalDateTime.now());
    match.setUpdatedAt(LocalDateTime.now());

    if (matchRepository.existsByHomeTeamAndAwayTeamAndSeason(
        match.getHomeTeam(), match.getAwayTeam(), match.getSeason())) {
      throw new MatchAlreadyExistsException("Match already exists!");
    }

    MatchEntity saved = matchRepository.save(match);
    log.info("Match created for {} vs {} (id={})", saved.getHomeTeam(), saved.getAwayTeam(),
        saved.getId());

    return modelMapper.map(saved, MatchDTO.class);
  }
}
