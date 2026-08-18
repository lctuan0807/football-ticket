package com.footballticket.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.service.ReservationService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ReservationService reservationService;

  @Test
  void createReservation_returnsSuccessTrue_whenReservationSucceeds() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(2);

    given(reservationService.createReservation(eq(1L), eq(10L), any(CreateReservationRequest.class)))
        .willReturn(true);

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code", is(200)))
        .andExpect(jsonPath("$.message", is("Reservation created successfully")))
        .andExpect(jsonPath("$.data", is(true)));
  }

  @Test
  void createReservation_returnsSuccessFalse_whenServiceReturnsFalse() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(2);

    given(reservationService.createReservation(eq(1L), eq(10L), any(CreateReservationRequest.class)))
        .willReturn(false);

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", is(false)));
  }

  @Test
  void createReservation_returnsConflict_whenNotEnoughStock() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(999);

    given(reservationService.createReservation(eq(1L), eq(10L), any(CreateReservationRequest.class)))
        .willThrow(new InsufficientTicketException(
            "Not enough stock available for ticket type: 10, available: 5, requested: 999"));

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is(409)))
        .andExpect(jsonPath("$.message",
            is("Not enough stock available for ticket type: 10, available: 5, requested: 999")));
  }

  @Test
  void createReservation_returnsBadRequest_whenQuantityMissing() throws Exception {
    String requestJson = "{}";

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createReservation_returnsBadRequest_whenQuantityIsZero() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(0);

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createReservation_returnsBadRequest_whenQuantityIsNegative() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(-5);

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
