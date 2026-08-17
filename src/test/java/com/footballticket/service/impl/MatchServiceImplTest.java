package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.dto.match.UpdateMatchRequest;
import com.footballticket.entity.MatchEntity;
import com.footballticket.enums.MatchStatusEnum;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.repository.MatchRepository;
import com.footballticket.service.RedisService;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

  @Mock
  private MatchRepository matchRepository;

  @Mock
  private ModelMapper modelMapper;

  @Mock
  private RedisService redisService;

  private MatchServiceImpl matchService;

  private final CreateMatchRequest request = new CreateMatchRequest(
      "Premier League",
      "1",
      "2025/2026",
      "Arsenal",
      "Chelsea",
      LocalDateTime.of(2026, 8, 20, 18, 0),
      "Emirates Stadium");

  @BeforeEach
  void setUp() {
    matchService = new MatchServiceImpl(matchRepository, modelMapper, redisService);
  }

  @Test
  void createMatch_savesMatchAndReturnsDto_whenMatchDoesNotAlreadyExist() {
    given(matchRepository.existsByHomeTeamAndAwayTeamAndSeason(
        request.homeTeam(), request.awayTeam(), request.season())).willReturn(false);

    MatchEntity saved = new MatchEntity();
    saved.setId(1L);
    saved.setHomeTeam(request.homeTeam());
    saved.setAwayTeam(request.awayTeam());
    given(matchRepository.save(any(MatchEntity.class))).willReturn(saved);

    MatchDTO expectedDto = new MatchDTO();
    expectedDto.setId(1L);
    given(modelMapper.map(saved, MatchDTO.class)).willReturn(expectedDto);

    MatchDTO result = matchService.createMatch(request);

    assertThat(result).isSameAs(expectedDto);

    ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
    verify(matchRepository).save(captor.capture());
    MatchEntity persisted = captor.getValue();
    assertThat(persisted.getCompetition()).isEqualTo(request.competition());
    assertThat(persisted.getStage()).isEqualTo(request.stage());
    assertThat(persisted.getSeason()).isEqualTo(request.season());
    assertThat(persisted.getHomeTeam()).isEqualTo(request.homeTeam());
    assertThat(persisted.getAwayTeam()).isEqualTo(request.awayTeam());
    assertThat(persisted.getKickoffAt()).isEqualTo(request.kickoffAt());
    assertThat(persisted.getStadium()).isEqualTo(request.stadium());
    assertThat(persisted.getStatus()).isEqualTo(MatchStatusEnum.SCHEDULED.toInt());
    assertThat(persisted.getCreatedAt()).isNotNull();
    assertThat(persisted.getUpdatedAt()).isNotNull();
  }

  @Test
  void createMatch_throwsResourceAlreadyExistsException_whenDuplicateMatchExists() {
    given(matchRepository.existsByHomeTeamAndAwayTeamAndSeason(
        request.homeTeam(), request.awayTeam(), request.season())).willReturn(true);

    assertThatThrownBy(() -> matchService.createMatch(request))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("Match already exists!");

    verify(matchRepository, never()).save(any(MatchEntity.class));
  }

  @Test
  void getMatch_returnsCachedDto_whenPresentInCache() {
    MatchDTO cachedDto = new MatchDTO();
    cachedDto.setId(1L);
    given(redisService.getObject("match:1", MatchDTO.class)).willReturn(cachedDto);

    MatchDTO result = matchService.getMatch(1L);

    assertThat(result).isSameAs(cachedDto);
    verify(matchRepository, never()).findById(any());
  }

  @Test
  void getMatch_fetchesFromRepositoryAndCachesResult_whenNotInCache() {
    given(redisService.getObject("match:1", MatchDTO.class)).willReturn(null);

    MatchEntity match = new MatchEntity();
    match.setId(1L);
    given(matchRepository.findById(1L)).willReturn(Optional.of(match));

    MatchDTO expectedDto = new MatchDTO();
    expectedDto.setId(1L);
    given(modelMapper.map(match, MatchDTO.class)).willReturn(expectedDto);

    MatchDTO result = matchService.getMatch(1L);

    assertThat(result).isSameAs(expectedDto);
    verify(redisService).setObject(eq("match:1"), eq(expectedDto), any(Duration.class));
  }

  @Test
  void getMatch_throwsResourceNotFoundException_whenMatchDoesNotExist() {
    given(redisService.getObject("match:1", MatchDTO.class)).willReturn(null);
    given(matchRepository.findById(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> matchService.getMatch(1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Match not found!");
  }

  @Test
  void getAllMatches_returnsAllMatches_whenStatusIsNull() {
    Pageable pageable = PageRequest.of(0, 10);
    MatchEntity match = new MatchEntity();
    match.setId(1L);
    given(matchRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(match)));

    MatchDTO expectedDto = new MatchDTO();
    expectedDto.setId(1L);
    given(modelMapper.map(match, MatchDTO.class)).willReturn(expectedDto);

    Page<MatchDTO> result = matchService.getAllMatches(pageable, null);

    assertThat(result.getContent()).containsExactly(expectedDto);
    verify(matchRepository, never()).findByStatus(any(), any());
  }

  @Test
  void getAllMatches_returnsFilteredMatches_whenStatusIsProvided() {
    Pageable pageable = PageRequest.of(0, 10);
    MatchEntity match = new MatchEntity();
    match.setId(1L);
    match.setStatus(MatchStatusEnum.FINISHED.toInt());
    given(matchRepository.findByStatus(eq(MatchStatusEnum.FINISHED.toInt()), eq(pageable)))
        .willReturn(new PageImpl<>(List.of(match)));

    MatchDTO expectedDto = new MatchDTO();
    expectedDto.setId(1L);
    given(modelMapper.map(match, MatchDTO.class)).willReturn(expectedDto);

    Page<MatchDTO> result = matchService.getAllMatches(pageable, MatchStatusEnum.FINISHED.toInt());

    assertThat(result.getContent()).containsExactly(expectedDto);
    verify(matchRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void updateMatch_updatesAndReturnsDto_whenMatchExists() {
    UpdateMatchRequest updateRequest = new UpdateMatchRequest(
        "La Liga",
        "2",
        "2025/2026",
        "Real Madrid",
        "Barcelona",
        LocalDateTime.of(2026, 9, 1, 20, 0),
        "Santiago Bernabeu");

    MatchEntity existing = new MatchEntity();
    existing.setId(1L);
    given(matchRepository.findById(1L)).willReturn(Optional.of(existing));
    given(matchRepository.save(any(MatchEntity.class))).willReturn(existing);

    MatchDTO expectedDto = new MatchDTO();
    expectedDto.setId(1L);
    given(modelMapper.map(existing, MatchDTO.class)).willReturn(expectedDto);

    MatchDTO result = matchService.updateMatch(1L, updateRequest);

    assertThat(result).isSameAs(expectedDto);

    ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
    verify(matchRepository).save(captor.capture());
    MatchEntity persisted = captor.getValue();
    assertThat(persisted.getCompetition()).isEqualTo(updateRequest.competition());
    assertThat(persisted.getStage()).isEqualTo(updateRequest.stage());
    assertThat(persisted.getSeason()).isEqualTo(updateRequest.season());
    assertThat(persisted.getHomeTeam()).isEqualTo(updateRequest.homeTeam());
    assertThat(persisted.getAwayTeam()).isEqualTo(updateRequest.awayTeam());
    assertThat(persisted.getKickoffAt()).isEqualTo(updateRequest.kickoffAt());
    assertThat(persisted.getStadium()).isEqualTo(updateRequest.stadium());
    assertThat(persisted.getUpdatedAt()).isNotNull();
    verify(redisService).delete("match:1");
  }

  @Test
  void updateMatch_throwsResourceNotFoundException_whenMatchDoesNotExist() {
    UpdateMatchRequest updateRequest = new UpdateMatchRequest(
        "La Liga",
        "2",
        "2025/2026",
        "Real Madrid",
        "Barcelona",
        LocalDateTime.of(2026, 9, 1, 20, 0),
        "Santiago Bernabeu");

    given(matchRepository.findById(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> matchService.updateMatch(1L, updateRequest))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Match not found!");

    verify(matchRepository, never()).save(any(MatchEntity.class));
  }

  @Test
  void deleteMatch_deletesMatchByIdAndEvictsCache() {
    matchService.deleteMatch(1L);

    verify(matchRepository).deleteById(1L);
    verify(redisService).delete("match:1");
  }
}
