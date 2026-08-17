package com.footballticket.service.impl;

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
import com.footballticket.service.TicketTypeService;
import com.footballticket.service.cache.TicketTypeCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketTypeServiceImpl implements TicketTypeService {
  private final TicketTypeRepository ticketTypeRepository;
  private final MatchRepository matchRepository;
  private final ModelMapper modelMapper;
  private final TicketTypeCacheService ticketTypeCacheService;

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
    return ticketTypeCacheService.getTicketType(id);
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
    ticketTypeCacheService.deleteTicketTypeFromCache(id);
    log.info("Ticket type updated: {} (id={})", saved.getName(), saved.getId());
    return toDto(saved);
  }

  @Override
  public void deleteTicketType(Long id) {
    ticketTypeRepository.deleteById(id);
    ticketTypeCacheService.deleteTicketTypeFromCache(id);
  }

  private TicketTypeDTO toDto(TicketTypeEntity entity) {
    TicketTypeDTO dto = modelMapper.map(entity, TicketTypeDTO.class);
    dto.setMatchId(entity.getMatch().getId());
    return dto;
  }
}
