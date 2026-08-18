package com.footballticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.repository.TicketTypeRepository;

import jakarta.persistence.PessimisticLockException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

  @Mock
  private TicketTypeRepository ticketTypeRepository;

  private ReservationServiceImpl reservationService;

  @BeforeEach
  void setUp() {
    reservationService = new ReservationServiceImpl(ticketTypeRepository);
  }

  @Test
  void createReservation_returnsTrue_whenStockSufficientAndReserveSucceeds() {
    given(ticketTypeRepository.getAvailableQuantity(10L)).willReturn(50);
    given(ticketTypeRepository.reserve(10L, 2)).willReturn(1);

    boolean result = reservationService.createReservation(1L, 10L, new CreateReservationRequest(2));

    assertThat(result).isTrue();
  }

  @Test
  void createReservation_returnsFalse_whenReserveUpdatesNoRows() {
    given(ticketTypeRepository.getAvailableQuantity(10L)).willReturn(50);
    given(ticketTypeRepository.reserve(10L, 2)).willReturn(0);

    boolean result = reservationService.createReservation(1L, 10L, new CreateReservationRequest(2));

    assertThat(result).isFalse();
  }

  @Test
  void createReservation_throwsInsufficientTicketException_whenAvailableQuantityLessThanRequested() {
    given(ticketTypeRepository.getAvailableQuantity(10L)).willReturn(1);

    assertThatThrownBy(() -> reservationService.createReservation(1L, 10L, new CreateReservationRequest(5)))
        .isInstanceOf(InsufficientTicketException.class)
        .hasMessage("Not enough stock available for ticket type: 10, available: 1, requested: 5");

    verify(ticketTypeRepository, never()).reserve(10L, 5);
  }

  @Test
  void createReservation_returnsFalse_whenPessimisticLockExceptionThrown() {
    given(ticketTypeRepository.getAvailableQuantity(10L)).willReturn(50);
    given(ticketTypeRepository.reserve(10L, 2)).willThrow(new PessimisticLockException("locked"));

    boolean result = reservationService.createReservation(1L, 10L, new CreateReservationRequest(2));

    assertThat(result).isFalse();
  }
}
