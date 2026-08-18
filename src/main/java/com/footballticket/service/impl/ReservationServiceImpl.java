package com.footballticket.service.impl;

import org.springframework.stereotype.Service;

import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.dto.reservation.ReservationDTO;
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.repository.TicketTypeRepository;
import com.footballticket.service.ReservationService;

import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

  private final TicketTypeRepository ticketTypeRepository;

  @Override
  public boolean createReservation(Long matchId, Long ticketTypeId, CreateReservationRequest request) {
    try {
      int quantity = request.quantity();
      log.info("Creating reservation for matchId: {}, ticketTypeId: {}, quantity: {}", matchId, ticketTypeId, quantity);
      int availableQuantity = ticketTypeRepository.getAvailableQuantity(ticketTypeId);
      log.info("Available quantity for ticketTypeId: {}: {}", ticketTypeId, availableQuantity);

      if (availableQuantity < quantity) {
        // throw new RuntimeException("Not enough stock available");
        log.info("availableQuantity < quantity: {} < {}", availableQuantity, quantity);
        throw new InsufficientTicketException("Not enough stock available for ticket type: " + ticketTypeId
            + ", available: " + availableQuantity + ", requested: " + quantity);
        // return false;
      }
      boolean result = ticketTypeRepository.reserve(ticketTypeId, quantity) > 0;
      log.info("Reservation result for ticketTypeId: {}: {}", ticketTypeId, result);
      return result;
    } catch (PessimisticLockException e) {
      log.error("Pessimistic lock exception occurred", e);
      // throw new RuntimeException("Reservation failed due to concurrent access", e);
      return false;
    }
  }

  @Override
  public ReservationDTO getReservation(Long id) {
    throw new UnsupportedOperationException("Unimplemented method 'getReservation'");
  }

  @Override
  public ReservationDTO confirmReservation(Long id) {
    throw new UnsupportedOperationException("Unimplemented method 'confirmReservation'");
  }

  @Override
  public ReservationDTO cancelReservation(Long id) {
    throw new UnsupportedOperationException("Unimplemented method 'cancelReservation'");
  }

}
