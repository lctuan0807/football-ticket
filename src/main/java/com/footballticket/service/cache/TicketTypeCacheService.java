package com.footballticket.service.cache;

import java.time.Duration;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.footballticket.dto.tickettype.TicketTypeDTO;
import com.footballticket.entity.TicketTypeEntity;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.repository.TicketTypeRepository;
import com.footballticket.service.RedisService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Service
@Slf4j
public class TicketTypeCacheService {
  private static final String TICKET_TYPE_CACHE_KEY_PREFIX = "ticketType:";
  private static final Duration TICKET_TYPE_CACHE_TTL = Duration.ofMinutes(10);

  private final RedisService redisService;
  private final TicketTypeRepository ticketTypeRepository;
  private final ModelMapper modelMapper;

  public TicketTypeDTO getTicketType(Long id) {
    String cacheKey = getCacheKey(id);

    TicketTypeDTO cached = redisService.getObject(cacheKey, TicketTypeDTO.class);
    if (cached != null) {
      return cached;
    }

    log.info("Ticket type not found in cache: {}", id);
    TicketTypeEntity ticketType = ticketTypeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found!"));

    TicketTypeDTO dto = toDto(ticketType);
    redisService.setObject(cacheKey, dto, TICKET_TYPE_CACHE_TTL);
    return dto;
  }

  public void deleteTicketTypeFromCache(Long id) {
    redisService.delete(getCacheKey(id));
  }

  private TicketTypeDTO toDto(TicketTypeEntity entity) {
    TicketTypeDTO dto = modelMapper.map(entity, TicketTypeDTO.class);
    dto.setMatchId(entity.getMatch().getId());
    return dto;
  }

  private String getCacheKey(Long id) {
    return TICKET_TYPE_CACHE_KEY_PREFIX + id;
  }
}
