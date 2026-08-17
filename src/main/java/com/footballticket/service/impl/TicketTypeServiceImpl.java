package com.footballticket.service.impl;

import java.time.Duration;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.footballticket.dto.tickettype.CreateTicketTypeRequest;
import com.footballticket.dto.tickettype.TicketTypeDTO;
import com.footballticket.dto.tickettype.UpdateTicketTypeRequest;
import com.footballticket.entity.MatchEntity;
import com.footballticket.entity.TicketTypeEntity;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.repository.MatchRepository;
import com.footballticket.repository.TicketTypeRepository;
import com.footballticket.service.RedisService;
import com.footballticket.service.TicketTypeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketTypeServiceImpl implements TicketTypeService {
  private static final String TICKET_TYPE_CACHE_KEY_PREFIX = "ticketType:";
  private static final Duration TICKET_TYPE_CACHE_TTL = Duration.ofMinutes(10);

  private final TicketTypeRepository ticketTypeRepository;
  private final MatchRepository matchRepository;
  private final ModelMapper modelMapper;
  private final RedisService redisService;

  @Override
  public TicketTypeDTO createTicketType(CreateTicketTypeRequest request) {
    MatchEntity match = matchRepository.findById(request.matchId())
        .orElseThrow(() -> new ResourceNotFoundException("Match not found!"));

    if (ticketTypeRepository.existsByMatch_IdAndName(request.matchId(), request.name())) {
      throw new ResourceAlreadyExistsException("Ticket type already exists for this match!");
    }

    TicketTypeEntity ticketType = new TicketTypeEntity();
    ticketType.setMatch(match);
    ticketType.setName(request.name());
    ticketType.setDescription(request.description());
    ticketType.setPrice(request.price());
    ticketType.setQuantity(request.quantity());
    ticketType.setAvailableQuantity(request.quantity());

    TicketTypeEntity saved = ticketTypeRepository.save(ticketType);
    log.info("Ticket type created: {} for match id={} (id={})", saved.getName(), match.getId(), saved.getId());
    return toDto(saved);
  }

  @Override
  public TicketTypeDTO getTicketType(Long id) {
    String cacheKey = TICKET_TYPE_CACHE_KEY_PREFIX + id;

    TicketTypeDTO cached = redisService.getObject(cacheKey, TicketTypeDTO.class);
    if (cached != null) {
      log.debug("Cache hit for ticket type id={}", id);
      return cached;
    }

    log.debug("Cache miss for ticket type id={}", id);
    TicketTypeEntity ticketType = ticketTypeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found!"));
    TicketTypeDTO dto = toDto(ticketType);
    redisService.setObject(cacheKey, dto, TICKET_TYPE_CACHE_TTL);
    log.debug("Populated cache for ticket type id={}", id);
    return dto;
  }

  @Override
  public List<TicketTypeDTO> getAllTicketTypes(Long matchId) {
    log.info("Getting all ticket types for match id={}", matchId);
    List<TicketTypeEntity> ticketTypes = matchId != null
        ? ticketTypeRepository.findByMatchId(matchId)
        : ticketTypeRepository.findAll();
    return ticketTypes.stream().map(this::toDto).toList();
  }

  @Override
  public TicketTypeDTO updateTicketType(Long id, UpdateTicketTypeRequest request) {
    TicketTypeEntity ticketType = ticketTypeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found!"));

    int sold = ticketType.getQuantity() - ticketType.getAvailableQuantity();
    ticketType.setName(request.name());
    ticketType.setDescription(request.description());
    ticketType.setPrice(request.price());
    ticketType.setQuantity(request.quantity());
    ticketType.setAvailableQuantity(Math.max(0, request.quantity() - sold));

    TicketTypeEntity saved = ticketTypeRepository.save(ticketType);
    redisService.delete(TICKET_TYPE_CACHE_KEY_PREFIX + id);
    log.info("Ticket type updated: {} (id={})", saved.getName(), saved.getId());
    return toDto(saved);
  }

  @Override
  public void deleteTicketType(Long id) {
    ticketTypeRepository.deleteById(id);
    redisService.delete(TICKET_TYPE_CACHE_KEY_PREFIX + id);
  }

  private TicketTypeDTO toDto(TicketTypeEntity entity) {
    TicketTypeDTO dto = modelMapper.map(entity, TicketTypeDTO.class);
    dto.setMatchId(entity.getMatch().getId());
    return dto;
  }
}
