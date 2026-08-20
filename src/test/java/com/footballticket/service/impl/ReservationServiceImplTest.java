package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.dto.reservation.ReservationDTO;
import com.footballticket.entity.MatchEntity;
import com.footballticket.entity.ReservationEntity;
import com.footballticket.entity.TicketTypeEntity;
import com.footballticket.enums.ReservationStatusEnum;
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.exceptions.InvalidReservationStateException;
import com.footballticket.exceptions.ReservationCreationFailedException;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.repository.ReservationRepository;
import com.footballticket.repository.TicketTypeRepository;

import jakarta.persistence.PessimisticLockException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

  @Mock
  private TicketTypeRepository ticketTypeRepository;

  @Mock
  private ReservationRepository reservationRepository;

  private final ModelMapper modelMapper = new ModelMapper();

  private ReservationServiceImpl reservationService;

  @BeforeEach
  void setUp() {
    reservationService = new ReservationServiceImpl(ticketTypeRepository, reservationRepository, modelMapper);
  }

  @Test
  void createReservation_persistsReservationAndReturnsDto_whenStockSufficientAndReserveSucceeds() {
    given(ticketTypeRepository.getAvailableQuantity(10L)).willReturn(50);
    given(ticketTypeRepository.reserve(10L, 2)).willReturn(1);

    ReservationEntity saved = new ReservationEntity();
    saved.setId(100L);
    saved.setUserId(1L);
    saved.setTicketTypeId(10L);
    saved.setStatus(0);
    saved.setQuantity(2);
    saved.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    given(reservationRepository.save(any(ReservationEntity.class))).willReturn(saved);

    ReservationDTO result = reservationService.createReservation(1L, 10L, new CreateReservationRequest(1L, 2));

    assertThat(result.getId()).isEqualTo(100L);
    assertThat(result.getUserId()).isEqualTo(1L);
    assertThat(result.getTicketTypeId()).isEqualTo(10L);
    assertThat(result.getMatchId()).isEqualTo(1L);
    assertThat(result.getQuantity()).isEqualTo(2);
    assertThat(result.getStatus()).isEqualTo("PENDING");

    ArgumentCaptor<ReservationEntity> captor = ArgumentCaptor.forClass(ReservationEntity.class);
    verify(reservationRepository).save(captor.capture());
    ReservationEntity persisted = captor.getValue();
    assertThat(persisted.getUserId()).isEqualTo(1L);
    assertThat(persisted.getTicketTypeId()).isEqualTo(10L);
    assertThat(persisted.getQuantity()).isEqualTo(2);
    assertThat(persisted.getStatus()).isEqualTo(0);
    assertThat(persisted.getExpiresAt()).isCloseTo(LocalDateTime.now().plusMinutes(15), within(5, ChronoUnit.SECONDS));
  }

  @Test
  void createReservation_throwsReservationCreationFailedException_whenReserveUpdatesNoRows() {
    given(ticketTypeRepository.getAvailableQuantity(10L)).willReturn(50);
    given(ticketTypeRepository.reserve(10L, 2)).willReturn(0);

    assertThatThrownBy(() -> reservationService.createReservation(1L, 10L, new CreateReservationRequest(1L, 2)))
        .isInstanceOf(ReservationCreationFailedException.class);

    verify(reservationRepository, never()).save(any(ReservationEntity.class));
  }

  @Test
  void createReservation_throwsInsufficientTicketException_whenAvailableQuantityLessThanRequested() {
    given(ticketTypeRepository.getAvailableQuantity(10L)).willReturn(1);

    assertThatThrownBy(() -> reservationService.createReservation(1L, 10L, new CreateReservationRequest(1L, 5)))
        .isInstanceOf(InsufficientTicketException.class)
        .hasMessage("Not enough stock available for ticket type: 10, available: 1, requested: 5");

    verify(ticketTypeRepository, never()).reserve(10L, 5);
    verify(reservationRepository, never()).save(any(ReservationEntity.class));
  }

  @Test
  void createReservation_throwsReservationCreationFailedException_whenPessimisticLockExceptionThrown() {
    given(ticketTypeRepository.getAvailableQuantity(10L)).willReturn(50);
    given(ticketTypeRepository.reserve(10L, 2)).willThrow(new PessimisticLockException("locked"));

    assertThatThrownBy(() -> reservationService.createReservation(1L, 10L, new CreateReservationRequest(1L, 2)))
        .isInstanceOf(ReservationCreationFailedException.class);

    verify(reservationRepository, never()).save(any(ReservationEntity.class));
  }

  @Test
  void cancelReservation_releasesStockAndReturnsCancelledDto_whenReservationIsPending() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(100L);
    reservation.setUserId(1L);
    reservation.setTicketTypeId(10L);
    reservation.setStatus(ReservationStatusEnum.PENDING.toInt());
    reservation.setQuantity(3);
    reservation.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    given(reservationRepository.findById(100L)).willReturn(Optional.of(reservation));
    given(reservationRepository.updateStatusIfCurrentStatus(100L, ReservationStatusEnum.PENDING.toInt(),
        ReservationStatusEnum.CANCELLED.toInt())).willReturn(1);

    MatchEntity match = new MatchEntity();
    match.setId(1L);
    TicketTypeEntity ticketType = new TicketTypeEntity();
    ticketType.setId(10L);
    ticketType.setMatch(match);
    given(ticketTypeRepository.findById(10L)).willReturn(Optional.of(ticketType));

    ReservationDTO result = reservationService.cancelReservation(100L);

    assertThat(result.getId()).isEqualTo(100L);
    assertThat(result.getMatchId()).isEqualTo(1L);
    assertThat(result.getStatus()).isEqualTo("CANCELLED");

    ArgumentCaptor<Long> ticketTypeIdCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> quantityCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(ticketTypeRepository).release(ticketTypeIdCaptor.capture(), quantityCaptor.capture());
    assertThat(ticketTypeIdCaptor.getValue()).isEqualTo(10L);
    assertThat(quantityCaptor.getValue()).isEqualTo(3);
  }

  @Test
  void cancelReservation_throwsResourceNotFoundException_whenReservationMissing() {
    given(reservationRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reservationService.cancelReservation(999L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(ticketTypeRepository, never()).release(any(), anyInt());
  }

  @Test
  void cancelReservation_throwsInvalidReservationStateException_whenAlreadyConfirmed() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(100L);
    reservation.setStatus(ReservationStatusEnum.CONFIRMED.toInt());
    given(reservationRepository.findById(100L)).willReturn(Optional.of(reservation));

    assertThatThrownBy(() -> reservationService.cancelReservation(100L))
        .isInstanceOf(InvalidReservationStateException.class)
        .hasMessageContaining("CONFIRMED");

    verify(reservationRepository, never()).updateStatusIfCurrentStatus(any(), any(), any());
    verify(ticketTypeRepository, never()).release(any(), anyInt());
  }

  @Test
  void cancelReservation_throwsInvalidReservationStateException_whenAlreadyCancelled() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(100L);
    reservation.setStatus(ReservationStatusEnum.CANCELLED.toInt());
    given(reservationRepository.findById(100L)).willReturn(Optional.of(reservation));

    assertThatThrownBy(() -> reservationService.cancelReservation(100L))
        .isInstanceOf(InvalidReservationStateException.class)
        .hasMessageContaining("CANCELLED");

    verify(ticketTypeRepository, never()).release(any(), anyInt());
  }

  @Test
  void cancelReservation_throwsInvalidReservationStateException_whenExpired() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(100L);
    reservation.setStatus(ReservationStatusEnum.EXPIRED.toInt());
    given(reservationRepository.findById(100L)).willReturn(Optional.of(reservation));

    assertThatThrownBy(() -> reservationService.cancelReservation(100L))
        .isInstanceOf(InvalidReservationStateException.class)
        .hasMessageContaining("EXPIRED");

    verify(ticketTypeRepository, never()).release(any(), anyInt());
  }

  @Test
  void cancelReservation_throwsInvalidReservationStateException_whenConcurrentTransitionLosesRace() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(100L);
    reservation.setTicketTypeId(10L);
    reservation.setQuantity(3);
    reservation.setStatus(ReservationStatusEnum.PENDING.toInt());
    given(reservationRepository.findById(100L)).willReturn(Optional.of(reservation));
    given(reservationRepository.updateStatusIfCurrentStatus(100L, ReservationStatusEnum.PENDING.toInt(),
        ReservationStatusEnum.CANCELLED.toInt())).willReturn(0);

    assertThatThrownBy(() -> reservationService.cancelReservation(100L))
        .isInstanceOf(InvalidReservationStateException.class);

    verify(ticketTypeRepository, never()).release(any(), anyInt());
  }

  @Test
  void getReservation_returnsDto_whenExists() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(100L);
    reservation.setUserId(1L);
    reservation.setTicketTypeId(10L);
    reservation.setStatus(ReservationStatusEnum.PENDING.toInt());
    reservation.setQuantity(2);
    reservation.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    given(reservationRepository.findById(100L)).willReturn(Optional.of(reservation));

    MatchEntity match = new MatchEntity();
    match.setId(1L);
    TicketTypeEntity ticketType = new TicketTypeEntity();
    ticketType.setId(10L);
    ticketType.setMatch(match);
    given(ticketTypeRepository.findById(10L)).willReturn(Optional.of(ticketType));

    ReservationDTO result = reservationService.getReservation(100L);

    assertThat(result.getId()).isEqualTo(100L);
    assertThat(result.getUserId()).isEqualTo(1L);
    assertThat(result.getTicketTypeId()).isEqualTo(10L);
    assertThat(result.getMatchId()).isEqualTo(1L);
    assertThat(result.getQuantity()).isEqualTo(2);
    assertThat(result.getStatus()).isEqualTo("PENDING");
  }

  @Test
  void getReservation_throwsResourceNotFoundException_whenReservationMissing() {
    given(reservationRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reservationService.getReservation(999L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(ticketTypeRepository, never()).findById(any());
  }

  @Test
  void getReservation_throwsResourceNotFoundException_whenTicketTypeMissing() {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(100L);
    reservation.setTicketTypeId(10L);
    reservation.setStatus(ReservationStatusEnum.PENDING.toInt());
    given(reservationRepository.findById(100L)).willReturn(Optional.of(reservation));
    given(ticketTypeRepository.findById(10L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reservationService.getReservation(100L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
