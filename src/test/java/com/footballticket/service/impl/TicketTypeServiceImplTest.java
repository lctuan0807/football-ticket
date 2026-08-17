package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.footballticket.dto.tickettype.CreateTicketTypeRequest;
import com.footballticket.dto.tickettype.TicketTypeDTO;
import com.footballticket.dto.tickettype.UpdateTicketTypeRequest;
import com.footballticket.entity.MatchEntity;
import com.footballticket.entity.TicketTypeEntity;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.repository.MatchRepository;
import com.footballticket.repository.TicketTypeRepository;
import com.footballticket.service.cache.TicketTypeCacheService;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceImplTest {

  @Mock
  private TicketTypeRepository ticketTypeRepository;

  @Mock
  private MatchRepository matchRepository;

  @Mock
  private ModelMapper modelMapper;

  @Mock
  private TicketTypeCacheService ticketTypeCacheService;

  private TicketTypeServiceImpl ticketTypeService;

  private final CreateTicketTypeRequest request = new CreateTicketTypeRequest(1L, "VIP", "Best seats", 500, 100);

  @BeforeEach
  void setUp() {
    ticketTypeService = new TicketTypeServiceImpl(ticketTypeRepository, matchRepository, modelMapper,
        ticketTypeCacheService);
  }

  @Test
  void createTicketType_savesTicketTypeAndReturnsDto_whenMatchExistsAndNameNotDuplicate() {
    MatchEntity match = new MatchEntity();
    match.setId(1L);
    given(matchRepository.findById(1L)).willReturn(Optional.of(match));
    given(ticketTypeRepository.existsByMatch_IdAndName(1L, "VIP")).willReturn(false);

    TicketTypeEntity saved = new TicketTypeEntity();
    saved.setId(10L);
    saved.setMatch(match);
    given(ticketTypeRepository.save(any(TicketTypeEntity.class))).willReturn(saved);

    TicketTypeDTO expectedDto = new TicketTypeDTO();
    expectedDto.setId(10L);
    given(modelMapper.map(saved, TicketTypeDTO.class)).willReturn(expectedDto);

    TicketTypeDTO result = ticketTypeService.createTicketType(request);

    assertThat(result).isSameAs(expectedDto);
    assertThat(result.getMatchId()).isEqualTo(1L);

    ArgumentCaptor<TicketTypeEntity> captor = ArgumentCaptor.forClass(TicketTypeEntity.class);
    verify(ticketTypeRepository).save(captor.capture());
    TicketTypeEntity persisted = captor.getValue();
    assertThat(persisted.getMatch()).isSameAs(match);
    assertThat(persisted.getName()).isEqualTo(request.name());
    assertThat(persisted.getDescription()).isEqualTo(request.description());
    assertThat(persisted.getPrice()).isEqualTo(request.price());
    assertThat(persisted.getQuantity()).isEqualTo(request.quantity());
    assertThat(persisted.getAvailableQuantity()).isEqualTo(request.quantity());
  }

  @Test
  void createTicketType_throwsResourceNotFoundException_whenMatchDoesNotExist() {
    given(matchRepository.findById(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> ticketTypeService.createTicketType(request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Match not found!");

    verify(ticketTypeRepository, never()).save(any(TicketTypeEntity.class));
  }

  @Test
  void createTicketType_throwsResourceAlreadyExistsException_whenDuplicateNameInMatch() {
    MatchEntity match = new MatchEntity();
    match.setId(1L);
    given(matchRepository.findById(1L)).willReturn(Optional.of(match));
    given(ticketTypeRepository.existsByMatch_IdAndName(1L, "VIP")).willReturn(true);

    assertThatThrownBy(() -> ticketTypeService.createTicketType(request))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("Ticket type already exists for this match!");

    verify(ticketTypeRepository, never()).save(any(TicketTypeEntity.class));
  }

  @Test
  void getTicketType_delegatesToCacheService() {
    TicketTypeDTO dto = new TicketTypeDTO();
    dto.setId(10L);
    given(ticketTypeCacheService.getTicketType(10L)).willReturn(dto);

    TicketTypeDTO result = ticketTypeService.getTicketType(10L);

    assertThat(result).isSameAs(dto);
  }

  @Test
  void getTicketType_propagatesResourceNotFoundException_whenCacheServiceThrows() {
    given(ticketTypeCacheService.getTicketType(10L))
        .willThrow(new ResourceNotFoundException("Ticket type not found!"));

    assertThatThrownBy(() -> ticketTypeService.getTicketType(10L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Ticket type not found!");
  }

  @Test
  void getAllTicketTypes_returnsAll_whenMatchIdIsNull() {
    MatchEntity match = new MatchEntity();
    match.setId(1L);
    TicketTypeEntity ticketType = new TicketTypeEntity();
    ticketType.setId(10L);
    ticketType.setMatch(match);
    given(ticketTypeRepository.findAll()).willReturn(List.of(ticketType));

    TicketTypeDTO expectedDto = new TicketTypeDTO();
    expectedDto.setId(10L);
    given(modelMapper.map(ticketType, TicketTypeDTO.class)).willReturn(expectedDto);

    List<TicketTypeDTO> result = ticketTypeService.getAllTicketTypes(null);

    assertThat(result).containsExactly(expectedDto);
    verify(ticketTypeRepository, never()).findByMatchId(any());
  }

  @Test
  void getAllTicketTypes_returnsFiltered_whenMatchIdProvided() {
    MatchEntity match = new MatchEntity();
    match.setId(1L);
    TicketTypeEntity ticketType = new TicketTypeEntity();
    ticketType.setId(10L);
    ticketType.setMatch(match);
    given(ticketTypeRepository.findByMatchId(1L)).willReturn(List.of(ticketType));

    TicketTypeDTO expectedDto = new TicketTypeDTO();
    expectedDto.setId(10L);
    given(modelMapper.map(ticketType, TicketTypeDTO.class)).willReturn(expectedDto);

    List<TicketTypeDTO> result = ticketTypeService.getAllTicketTypes(1L);

    assertThat(result).containsExactly(expectedDto);
    verify(ticketTypeRepository, never()).findAll();
  }

  @Test
  void updateTicketType_updatesFieldsAndPreservesSoldCount_whenExists() {
    MatchEntity match = new MatchEntity();
    match.setId(1L);
    TicketTypeEntity existing = new TicketTypeEntity();
    existing.setId(10L);
    existing.setMatch(match);
    existing.setQuantity(100);
    existing.setAvailableQuantity(40);
    given(ticketTypeRepository.findById(10L)).willReturn(Optional.of(existing));
    given(ticketTypeRepository.save(any(TicketTypeEntity.class))).willReturn(existing);

    TicketTypeDTO expectedDto = new TicketTypeDTO();
    expectedDto.setId(10L);
    given(modelMapper.map(existing, TicketTypeDTO.class)).willReturn(expectedDto);

    UpdateTicketTypeRequest updateRequest = new UpdateTicketTypeRequest("Category 1", "Mid-tier seats", 300, 150);
    TicketTypeDTO result = ticketTypeService.updateTicketType(10L, updateRequest);

    assertThat(result).isSameAs(expectedDto);
    assertThat(result.getMatchId()).isEqualTo(1L);

    ArgumentCaptor<TicketTypeEntity> captor = ArgumentCaptor.forClass(TicketTypeEntity.class);
    verify(ticketTypeRepository).save(captor.capture());
    TicketTypeEntity persisted = captor.getValue();
    assertThat(persisted.getName()).isEqualTo("Category 1");
    assertThat(persisted.getDescription()).isEqualTo("Mid-tier seats");
    assertThat(persisted.getPrice()).isEqualTo(300);
    assertThat(persisted.getQuantity()).isEqualTo(150);
    assertThat(persisted.getAvailableQuantity()).isEqualTo(90);
    verify(ticketTypeCacheService).deleteTicketTypeFromCache(10L);
  }

  @Test
  void updateTicketType_clampsAvailableQuantityToZero_whenNewQuantityLessThanSold() {
    MatchEntity match = new MatchEntity();
    match.setId(1L);
    TicketTypeEntity existing = new TicketTypeEntity();
    existing.setId(10L);
    existing.setMatch(match);
    existing.setQuantity(100);
    existing.setAvailableQuantity(10);
    given(ticketTypeRepository.findById(10L)).willReturn(Optional.of(existing));
    given(ticketTypeRepository.save(any(TicketTypeEntity.class))).willReturn(existing);
    given(modelMapper.map(existing, TicketTypeDTO.class)).willReturn(new TicketTypeDTO());

    UpdateTicketTypeRequest updateRequest = new UpdateTicketTypeRequest("VIP", null, 500, 50);
    ticketTypeService.updateTicketType(10L, updateRequest);

    ArgumentCaptor<TicketTypeEntity> captor = ArgumentCaptor.forClass(TicketTypeEntity.class);
    verify(ticketTypeRepository).save(captor.capture());
    assertThat(captor.getValue().getAvailableQuantity()).isEqualTo(0);
  }

  @Test
  void updateTicketType_throwsResourceNotFoundException_whenNotExists() {
    given(ticketTypeRepository.findById(10L)).willReturn(Optional.empty());

    UpdateTicketTypeRequest updateRequest = new UpdateTicketTypeRequest("VIP", null, 500, 50);

    assertThatThrownBy(() -> ticketTypeService.updateTicketType(10L, updateRequest))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Ticket type not found!");

    verify(ticketTypeRepository, never()).save(any(TicketTypeEntity.class));
  }

  @Test
  void deleteTicketType_deletesByIdAndEvictsCache() {
    ticketTypeService.deleteTicketType(10L);

    verify(ticketTypeRepository).deleteById(10L);
    verify(ticketTypeCacheService).deleteTicketTypeFromCache(10L);
  }
}
