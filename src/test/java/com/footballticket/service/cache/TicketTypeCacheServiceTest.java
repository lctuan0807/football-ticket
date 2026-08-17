package com.footballticket.service.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.footballticket.dto.tickettype.TicketTypeDTO;
import com.footballticket.entity.MatchEntity;
import com.footballticket.entity.TicketTypeEntity;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.repository.TicketTypeRepository;
import com.footballticket.service.RedisService;

@ExtendWith(MockitoExtension.class)
class TicketTypeCacheServiceTest {

  @Mock
  private RedisService redisService;

  @Mock
  private TicketTypeRepository ticketTypeRepository;

  @Mock
  private ModelMapper modelMapper;

  private TicketTypeCacheService ticketTypeCacheService;

  @BeforeEach
  void setUp() {
    ticketTypeCacheService = new TicketTypeCacheService(redisService, ticketTypeRepository, modelMapper);
  }

  @Test
  void getTicketType_returnsCachedDto_whenPresentInCache() {
    TicketTypeDTO cachedDto = new TicketTypeDTO();
    cachedDto.setId(10L);
    given(redisService.getObject("ticketType:10", TicketTypeDTO.class)).willReturn(cachedDto);

    TicketTypeDTO result = ticketTypeCacheService.getTicketType(10L);

    assertThat(result).isSameAs(cachedDto);
    verify(ticketTypeRepository, never()).findById(any());
  }

  @Test
  void getTicketType_fetchesFromRepositoryAndCachesDto_whenNotInCache() {
    given(redisService.getObject("ticketType:10", TicketTypeDTO.class)).willReturn(null);

    MatchEntity match = new MatchEntity();
    match.setId(1L);
    TicketTypeEntity ticketType = new TicketTypeEntity();
    ticketType.setId(10L);
    ticketType.setMatch(match);
    given(ticketTypeRepository.findById(10L)).willReturn(Optional.of(ticketType));

    TicketTypeDTO expectedDto = new TicketTypeDTO();
    expectedDto.setId(10L);
    given(modelMapper.map(ticketType, TicketTypeDTO.class)).willReturn(expectedDto);

    TicketTypeDTO result = ticketTypeCacheService.getTicketType(10L);

    assertThat(result).isSameAs(expectedDto);
    assertThat(result.getMatchId()).isEqualTo(1L);
    verify(redisService).setObject(eq("ticketType:10"), eq(expectedDto), any(Duration.class));
  }

  @Test
  void getTicketType_throwsResourceNotFoundException_whenNotExists() {
    given(redisService.getObject("ticketType:10", TicketTypeDTO.class)).willReturn(null);
    given(ticketTypeRepository.findById(10L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> ticketTypeCacheService.getTicketType(10L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Ticket type not found!");

    verify(redisService, never()).setObject(any(), any(), any());
  }

  @Test
  void deleteTicketTypeFromCache_deletesCacheKey() {
    ticketTypeCacheService.deleteTicketTypeFromCache(10L);

    verify(redisService).delete("ticketType:10");
  }
}
