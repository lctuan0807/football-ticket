package com.footballticket.service;

import java.util.List;

import com.footballticket.dto.tickettype.CreateTicketTypeRequest;
import com.footballticket.dto.tickettype.TicketTypeDTO;
import com.footballticket.dto.tickettype.UpdateTicketTypeRequest;

public interface TicketTypeService {
  TicketTypeDTO createTicketType(Long matchId, CreateTicketTypeRequest request);

  TicketTypeDTO getTicketType(Long matchId, Long id);

  List<TicketTypeDTO> getAllTicketTypes(Long matchId);

  TicketTypeDTO updateTicketType(Long matchId, Long id, UpdateTicketTypeRequest request);

  void deleteTicketType(Long matchId, Long id);
}
