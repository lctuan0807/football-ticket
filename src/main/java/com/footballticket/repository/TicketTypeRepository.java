package com.footballticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballticket.entity.TicketTypeEntity;

public interface TicketTypeRepository extends JpaRepository<TicketTypeEntity, Long> {

  boolean existsByMatchIdAndName(Long matchId, String name);

  List<TicketTypeEntity> findByMatchId(Long matchId);

  Optional<TicketTypeEntity> findByIdAndMatchId(Long id, Long matchId);
}
