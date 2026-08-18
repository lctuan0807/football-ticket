package com.footballticket.service;

import com.footballticket.dto.reservation.ReservationDTO;
import com.footballticket.dto.reservation.CreateReservationRequest;

public interface ReservationService {
  boolean createReservation(Long matchId, Long ticketTypeId, CreateReservationRequest request);

  ReservationDTO getReservation(Long id);

  ReservationDTO confirmReservation(Long id);

  ReservationDTO cancelReservation(Long id);
}
