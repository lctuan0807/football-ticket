package com.footballticket.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.footballticket.dto.tickettype.CreateTicketTypeRequest;
import com.footballticket.dto.tickettype.TicketTypeDTO;
import com.footballticket.dto.tickettype.UpdateTicketTypeRequest;
import com.footballticket.service.TicketTypeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ticket-types")
@Slf4j
public class TicketTypeController {
  private final TicketTypeService ticketTypeService;

  @PostMapping
  public ResponseEntity<TicketTypeDTO> createTicketType(@RequestBody @Valid CreateTicketTypeRequest request) {
    TicketTypeDTO ticketType = ticketTypeService.createTicketType(request);
    return ResponseEntity.ok(ticketType);
  }

  @GetMapping
  public ResponseEntity<List<TicketTypeDTO>> getTicketTypes(@RequestParam(required = false) Long matchId) {
    List<TicketTypeDTO> ticketTypes = ticketTypeService.getAllTicketTypes(matchId);
    return ResponseEntity.ok(ticketTypes);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TicketTypeDTO> getTicketType(@PathVariable Long id) {
    TicketTypeDTO ticketType = ticketTypeService.getTicketType(id);
    return ResponseEntity.ok(ticketType);
  }

  @PutMapping("/{id}")
  public ResponseEntity<TicketTypeDTO> updateTicketType(@PathVariable Long id,
      @RequestBody @Valid UpdateTicketTypeRequest request) {
    TicketTypeDTO ticketType = ticketTypeService.updateTicketType(id, request);
    return ResponseEntity.ok(ticketType);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTicketType(@PathVariable Long id) {
    ticketTypeService.deleteTicketType(id);
    return ResponseEntity.noContent().build();
  }
}
