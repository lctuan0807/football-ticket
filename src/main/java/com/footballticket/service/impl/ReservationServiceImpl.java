package com.footballticket.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.dto.reservation.ReservationDTO;
import com.footballticket.entity.ReservationEntity;
import com.footballticket.entity.TicketTypeEntity;
import com.footballticket.enums.ReservationStatusEnum;
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.exceptions.InvalidReservationStateException;
import com.footballticket.exceptions.ReservationCreationFailedException;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.repository.ReservationRepository;
import com.footballticket.repository.TicketTypeRepository;
import com.footballticket.service.ReservationService;

import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

  private static final Duration RESERVATION_HOLD_DURATION = Duration.ofMinutes(15);

  private final TicketTypeRepository ticketTypeRepository;
  private final ReservationRepository reservationRepository;
  private final ModelMapper modelMapper;

  @Override
  @Transactional
  public ReservationDTO createReservation(Long matchId, Long ticketTypeId, CreateReservationRequest request) {
    try {
      int quantity = request.quantity();
      log.info("Creating reservation for matchId: {}, ticketTypeId: {}, quantity: {}", matchId, ticketTypeId, quantity);
      int availableQuantity = ticketTypeRepository.getAvailableQuantity(ticketTypeId);
      log.info("Available quantity for ticketTypeId: {}: {}", ticketTypeId, availableQuantity);

      if (availableQuantity < quantity) {
        log.info("availableQuantity < quantity: {} < {}", availableQuantity, quantity);
        throw new InsufficientTicketException("Not enough stock available for ticket type: " + ticketTypeId
            + ", available: " + availableQuantity + ", requested: " + quantity);
      }

      boolean reserved = ticketTypeRepository.reserve(ticketTypeId, quantity) > 0;
      log.info("Reservation result for ticketTypeId: {}: {}", ticketTypeId, reserved);
      if (!reserved) {
        throw new ReservationCreationFailedException(
            "Could not reserve ticket type: " + ticketTypeId + " due to concurrent stock update");
      }

      ReservationEntity reservation = new ReservationEntity();
      reservation.setUserId(request.userId());
      reservation.setTicketTypeId(ticketTypeId);
      reservation.setStatus(ReservationStatusEnum.PENDING.toInt());
      reservation.setQuantity(quantity);
      reservation.setExpiresAt(LocalDateTime.now().plus(RESERVATION_HOLD_DURATION));

      ReservationEntity saved = reservationRepository.save(reservation);
      log.info("Reservation created: id={} for ticketTypeId={} userId={}", saved.getId(), ticketTypeId,
          request.userId());

      return toDto(saved, matchId);
    } catch (PessimisticLockException e) {
      log.error("Pessimistic lock exception occurred", e);
      throw new ReservationCreationFailedException(
          "Could not create reservation for ticket type: " + ticketTypeId + " due to a concurrent access conflict");
    }
  }

  @Override
  public ReservationDTO getReservation(Long id) {
    ReservationEntity reservation = reservationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));

    TicketTypeEntity ticketType = ticketTypeRepository.findById(reservation.getTicketTypeId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Ticket type not found: " + reservation.getTicketTypeId()));

    return toDto(reservation, ticketType.getMatch().getId());
  }

  @Override
  public ReservationDTO confirmReservation(Long id) {
    throw new UnsupportedOperationException("Unimplemented method 'confirmReservation'");
  }

  @Override
  @Transactional
  public ReservationDTO cancelReservation(Long id) {
    ReservationEntity reservation = reservationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));

    if (reservation.getStatus() != ReservationStatusEnum.PENDING.toInt()) {
      throw new InvalidReservationStateException(
          "Cannot cancel reservation " + id + " in status "
              + ReservationStatusEnum.values()[reservation.getStatus()].name());
    }

    int updated = reservationRepository.updateStatusIfCurrentStatus(
        id, ReservationStatusEnum.PENDING.toInt(), ReservationStatusEnum.CANCELLED.toInt());
    if (updated == 0) {
      throw new InvalidReservationStateException(
          "Reservation " + id + " was already transitioned by a concurrent request");
    }

    ticketTypeRepository.release(reservation.getTicketTypeId(), reservation.getQuantity());

    TicketTypeEntity ticketType = ticketTypeRepository.findById(reservation.getTicketTypeId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Ticket type not found: " + reservation.getTicketTypeId()));

    reservation.setStatus(ReservationStatusEnum.CANCELLED.toInt());
    return toDto(reservation, ticketType.getMatch().getId());
  }

  private ReservationDTO toDto(ReservationEntity entity, Long matchId) {
    ReservationDTO dto = modelMapper.map(entity, ReservationDTO.class);
    dto.setMatchId(matchId);
    dto.setStatus(ReservationStatusEnum.values()[entity.getStatus()].name());
    return dto;
  }

}
