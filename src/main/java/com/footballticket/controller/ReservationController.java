package com.footballticket.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballticket.common.ApiResponse;
import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.dto.reservation.ReservationDTO;
import com.footballticket.service.ReservationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReservationController {
  private final ReservationService reservationService;

  @PostMapping("/matches/{matchId}/ticket-types/{ticketTypeId}/reservations")
  public ResponseEntity<ApiResponse<ReservationDTO>> createReservation(@PathVariable Long matchId,
      @PathVariable Long ticketTypeId, @RequestBody @Valid CreateReservationRequest request) {
    log.info("Creating reservation for matchId: {}, ticketTypeId: {}, request: {}", matchId, ticketTypeId, request);
    ReservationDTO reservation = reservationService.createReservation(matchId, ticketTypeId, request);
    return ResponseEntity.ok(ApiResponse.success("Reservation created successfully", reservation));
  }

  @PatchMapping("/reservations/{id}/cancel")
  public ResponseEntity<ApiResponse<ReservationDTO>> cancelReservation(@PathVariable Long id) {
    log.info("Cancelling reservation id: {}", id);
    ReservationDTO reservation = reservationService.cancelReservation(id);
    return ResponseEntity.ok(ApiResponse.success("Reservation cancelled successfully", reservation));
  }

  @GetMapping("/reservations/{id}")
  public ResponseEntity<ApiResponse<ReservationDTO>> getReservation(@PathVariable Long id) {
    log.info("Getting reservation id: {}", id);
    ReservationDTO reservation = reservationService.getReservation(id);
    return ResponseEntity.ok(ApiResponse.success(reservation));
  }
}
