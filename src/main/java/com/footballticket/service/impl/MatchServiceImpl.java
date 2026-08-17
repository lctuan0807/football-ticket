package com.footballticket.service.impl;

import java.time.Duration;
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
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.repository.MatchRepository;
import com.footballticket.service.MatchService;
import com.footballticket.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
  private static final String MATCH_CACHE_KEY_PREFIX = "match:";
  private static final Duration MATCH_CACHE_TTL = Duration.ofMinutes(10);

  private static int cacheHitCount = 0;
  private static int cacheMissCount = 0;

  private final MatchRepository matchRepository;
  private final ModelMapper modelMapper;
  private final RedisService redisService;

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
      throw new ResourceAlreadyExistsException("Match already exists!");
    }

    MatchEntity saved = matchRepository.save(match);
    log.info("Match created for {} vs {} (id={})", saved.getHomeTeam(), saved.getAwayTeam(),
        saved.getId());

    return modelMapper.map(saved, MatchDTO.class);
  }

  // Get match by id
  @Override
  public MatchDTO getMatch(Long id) {
    String cacheKey = MATCH_CACHE_KEY_PREFIX + id;

    MatchDTO cached = redisService.getObject(cacheKey, MatchDTO.class);
    log.info("MatchService | getMatch | cached {}", cached);
    if (cached != null) {
      log.info("MatchService | getMatch | Cache hit | id: {}", id);
      cacheHitCount++;
      return cached;
    }

    log.info("MatchService | getMatch | Cache miss | Fetching match with id: {}", id);
    cacheMissCount++;
    redisService.setObject("cacheHitCount", cacheHitCount);
    redisService.setObject("cacheMissCount", cacheMissCount);
    MatchEntity match = matchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found!"));
    MatchDTO dto = modelMapper.map(match, MatchDTO.class);
    redisService.setObject(cacheKey, dto, MATCH_CACHE_TTL);
    return dto;
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
    redisService.delete(MATCH_CACHE_KEY_PREFIX + id);
    log.info("Match updated for {} vs {} (id={})", saved.getHomeTeam(), saved.getAwayTeam(),
        saved.getId());
    return modelMapper.map(saved, MatchDTO.class);
  }

  // delete a match
  @Override
  public void deleteMatch(Long id) {
    matchRepository.deleteById(id);
    redisService.delete(MATCH_CACHE_KEY_PREFIX + id);
  }
}
