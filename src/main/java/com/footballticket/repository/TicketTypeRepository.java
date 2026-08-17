package com.footballticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballticket.entity.TicketTypeEntity;

public interface TicketTypeRepository extends JpaRepository<TicketTypeEntity, Long> {

  boolean existsByMatch_IdAndName(Long matchId, String name);

  List<TicketTypeEntity> findByMatchId(Long matchId);
}
