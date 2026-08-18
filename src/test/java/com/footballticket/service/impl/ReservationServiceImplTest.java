package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.dto.reservation.ReservationDTO;
import com.footballticket.entity.ReservationEntity;
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.exceptions.ReservationCreationFailedException;
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
}
