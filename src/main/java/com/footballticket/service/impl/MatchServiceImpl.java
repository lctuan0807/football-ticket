package com.footballticket.service.impl;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.dto.match.UpdateMatchRequest;
import com.footballticket.entity.MatchEntity;
import com.footballticket.enums.MatchStatusEnum;
import com.footballticket.exceptions.MatchAlreadyExistsException;
import com.footballticket.exceptions.ResourceNotFoundException;
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

  // create a match
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

  // Get match by id
  @Override
  public MatchDTO getMatch(Long id) {
    MatchEntity match = matchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found!"));
    return modelMapper.map(match, MatchDTO.class);
  }

  // Get all matches with pagination, optionally filtered by status
  @Override
  public Page<MatchDTO> getAllMatches(Pageable pageable, Integer status) {
    Page<MatchEntity> matches = status != null
        ? matchRepository.findByStatus(status, pageable)
        : matchRepository.findAll(pageable);
    return matches.map(match -> modelMapper.map(match, MatchDTO.class));
  }

  // Update a match
  @Override
  public MatchDTO updateMatch(Long id, UpdateMatchRequest request) {
    MatchEntity match = matchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found!"));
    match.setCompetition(request.competition());
    match.setStage(request.stage());
    match.setSeason(request.season());
    match.setHomeTeam(request.homeTeam());
    match.setAwayTeam(request.awayTeam());
    match.setKickoffAt(request.kickoffAt());
    match.setStadium(request.stadium());
    match.setUpdatedAt(LocalDateTime.now());
    MatchEntity saved = matchRepository.save(match);
    log.info("Match updated for {} vs {} (id={})", saved.getHomeTeam(), saved.getAwayTeam(),
        saved.getId());
    return modelMapper.map(saved, MatchDTO.class);
  }

  // delete a match
  @Override
  public void deleteMatch(Long id) {
    matchRepository.deleteById(id);
  }
}
