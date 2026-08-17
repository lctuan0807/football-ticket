package com.footballticket.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.footballticket.dto.tickettype.CreateTicketTypeRequest;
import com.footballticket.dto.tickettype.TicketTypeDTO;
import com.footballticket.dto.tickettype.UpdateTicketTypeRequest;
import com.footballticket.exceptions.ResourceAlreadyExistsException;
import com.footballticket.exceptions.ResourceNotFoundException;
import com.footballticket.service.TicketTypeService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(TicketTypeController.class)
class TicketTypeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private TicketTypeService ticketTypeService;

  @Test
  void createTicketType_returnsCreatedTicketType_whenRequestIsValid() throws Exception {
    CreateTicketTypeRequest request = new CreateTicketTypeRequest(1L, "VIP", 500, 100);
    TicketTypeDTO response = new TicketTypeDTO(1L, 1L, "VIP", 500, 100, 100);

    given(ticketTypeService.createTicketType(any(CreateTicketTypeRequest.class))).willReturn(response);

    mockMvc.perform(post("/api/v1/ticket-types")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(1)))
        .andExpect(jsonPath("$.matchId", is(1)))
        .andExpect(jsonPath("$.name", is("VIP")));
  }

  @Test
  void createTicketType_returnsBadRequest_whenRequiredFieldIsMissing() throws Exception {
    String requestJson = """
        {
          "name": "VIP",
          "price": 500,
          "quantity": 100
        }
        """;

    mockMvc.perform(post("/api/v1/ticket-types")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createTicketType_returnsNotFound_whenMatchDoesNotExist() throws Exception {
    CreateTicketTypeRequest request = new CreateTicketTypeRequest(1L, "VIP", 500, 100);

    given(ticketTypeService.createTicketType(any(CreateTicketTypeRequest.class)))
        .willThrow(new ResourceNotFoundException("Match not found!"));

    mockMvc.perform(post("/api/v1/ticket-types")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void createTicketType_returnsConflict_whenDuplicateName() throws Exception {
    CreateTicketTypeRequest request = new CreateTicketTypeRequest(1L, "VIP", 500, 100);

    given(ticketTypeService.createTicketType(any(CreateTicketTypeRequest.class)))
        .willThrow(new ResourceAlreadyExistsException("Ticket type already exists for this match!"));

    mockMvc.perform(post("/api/v1/ticket-types")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void getTicketType_returnsTicketType_whenExists() throws Exception {
    TicketTypeDTO response = new TicketTypeDTO(1L, 1L, "VIP", 500, 100, 100);

    given(ticketTypeService.getTicketType(1L)).willReturn(response);

    mockMvc.perform(get("/api/v1/ticket-types/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(1)))
        .andExpect(jsonPath("$.name", is("VIP")));
  }

  @Test
  void getTicketType_returnsNotFound_whenNotExists() throws Exception {
    given(ticketTypeService.getTicketType(1L)).willThrow(new ResourceNotFoundException("Ticket type not found!"));

    mockMvc.perform(get("/api/v1/ticket-types/1"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTicketTypes_returnsAllResults_whenMatchIdNotProvided() throws Exception {
    TicketTypeDTO ticketType = new TicketTypeDTO(1L, 1L, "VIP", 500, 100, 100);
    given(ticketTypeService.getAllTicketTypes(isNull())).willReturn(List.of(ticketType));

    mockMvc.perform(get("/api/v1/ticket-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(1)));
  }

  @Test
  void getTicketTypes_returnsFilteredResults_whenMatchIdProvided() throws Exception {
    TicketTypeDTO ticketType = new TicketTypeDTO(1L, 1L, "VIP", 500, 100, 100);
    given(ticketTypeService.getAllTicketTypes(eq(1L))).willReturn(List.of(ticketType));

    mockMvc.perform(get("/api/v1/ticket-types").param("matchId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id", is(1)));
  }

  @Test
  void updateTicketType_returnsUpdatedTicketType_whenValid() throws Exception {
    UpdateTicketTypeRequest request = new UpdateTicketTypeRequest("Category 1", 300, 150);
    TicketTypeDTO response = new TicketTypeDTO(1L, 1L, "Category 1", 300, 150, 150);

    given(ticketTypeService.updateTicketType(eq(1L), any(UpdateTicketTypeRequest.class))).willReturn(response);

    mockMvc.perform(put("/api/v1/ticket-types/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Category 1")))
        .andExpect(jsonPath("$.price", is(300)));
  }

  @Test
  void updateTicketType_returnsNotFound_whenNotExists() throws Exception {
    UpdateTicketTypeRequest request = new UpdateTicketTypeRequest("Category 1", 300, 150);

    given(ticketTypeService.updateTicketType(eq(1L), any(UpdateTicketTypeRequest.class)))
        .willThrow(new ResourceNotFoundException("Ticket type not found!"));

    mockMvc.perform(put("/api/v1/ticket-types/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteTicketType_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/v1/ticket-types/1"))
        .andExpect(status().isNoContent());
  }
}
