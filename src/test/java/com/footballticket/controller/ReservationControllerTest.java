package com.footballticket.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.footballticket.dto.reservation.CreateReservationRequest;
import com.footballticket.dto.reservation.ReservationDTO;
import com.footballticket.exceptions.InsufficientTicketException;
import com.footballticket.exceptions.ReservationCreationFailedException;
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
  void createReservation_returnsCreatedReservation_whenReservationSucceeds() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(1L, 2);

    ReservationDTO response = new ReservationDTO();
    response.setId(100L);
    response.setUserId(1L);
    response.setTicketTypeId(10L);
    response.setMatchId(1L);
    response.setQuantity(2);
    response.setStatus("PENDING");
    response.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    response.setCreatedAt(LocalDateTime.now());

    given(reservationService.createReservation(eq(1L), eq(10L), any(CreateReservationRequest.class)))
        .willReturn(response);

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code", is(200)))
        .andExpect(jsonPath("$.message", is("Reservation created successfully")))
        .andExpect(jsonPath("$.data.id", is(100)))
        .andExpect(jsonPath("$.data.userId", is(1)))
        .andExpect(jsonPath("$.data.ticketTypeId", is(10)))
        .andExpect(jsonPath("$.data.matchId", is(1)))
        .andExpect(jsonPath("$.data.quantity", is(2)))
        .andExpect(jsonPath("$.data.status", is("PENDING")));
  }

  @Test
  void createReservation_returnsConflict_whenNotEnoughStock() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(1L, 999);

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
  void createReservation_returnsConflict_whenReservationCreationFails() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(1L, 2);

    given(reservationService.createReservation(eq(1L), eq(10L), any(CreateReservationRequest.class)))
        .willThrow(new ReservationCreationFailedException(
            "Could not reserve ticket type: 10 due to concurrent stock update"));

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is(409)))
        .andExpect(jsonPath("$.message", is("Could not reserve ticket type: 10 due to concurrent stock update")));
  }

  @Test
  void createReservation_returnsBadRequest_whenQuantityMissing() throws Exception {
    String requestJson = "{\"userId\": 1}";

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createReservation_returnsBadRequest_whenQuantityIsZero() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(1L, 0);

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createReservation_returnsBadRequest_whenQuantityIsNegative() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(1L, -5);

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createReservation_returnsBadRequest_whenUserIdMissing() throws Exception {
    String requestJson = "{\"quantity\": 2}";

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createReservation_returnsBadRequest_whenUserIdIsNotPositive() throws Exception {
    CreateReservationRequest request = new CreateReservationRequest(-1L, 2);

    mockMvc.perform(post("/api/v1/matches/1/ticket-types/10/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
