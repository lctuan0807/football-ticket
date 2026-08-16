package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.footballticket.dto.match.CreateMatchRequest;
import com.footballticket.dto.match.MatchDTO;
import com.footballticket.entity.MatchEntity;
import com.footballticket.enums.MatchStatusEnum;
import com.footballticket.exceptions.MatchAlreadyExistsException;
import com.footballticket.repository.MatchRepository;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

  @Mock
  private MatchRepository matchRepository;

  @Mock
  private ModelMapper modelMapper;

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
    matchService = new MatchServiceImpl(matchRepository, modelMapper);
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
  void createMatch_throwsMatchAlreadyExistsException_whenDuplicateMatchExists() {
    given(matchRepository.existsByHomeTeamAndAwayTeamAndSeason(
        request.homeTeam(), request.awayTeam(), request.season())).willReturn(true);

    assertThatThrownBy(() -> matchService.createMatch(request))
        .isInstanceOf(MatchAlreadyExistsException.class)
        .hasMessage("Match already exists!");

    verify(matchRepository, never()).save(any(MatchEntity.class));
  }
}
