package com.footballticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.footballticket.entity.TicketTypeEntity;

import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;

public interface TicketTypeRepository extends JpaRepository<TicketTypeEntity, Long> {

  boolean existsByMatchIdAndName(Long matchId, String name);

  List<TicketTypeEntity> findByMatchId(Long matchId);

  Optional<TicketTypeEntity> findByIdAndMatchId(Long id, Long matchId);

  @Query("SELECT tt.availableQuantity FROM TicketTypeEntity tt WHERE tt.id = :ticketTypeId")
  int getAvailableQuantity(Long ticketTypeId);

  @Modifying
  @Transactional
  // @Query("UPDATE TicketTypeEntity tt SET tt.availableQuantity =
  // tt.availableQuantity - :quantity WHERE tt.id = :ticketTypeId")
  @Query("UPDATE TicketTypeEntity tt SET tt.availableQuantity = tt.availableQuantity - :quantity WHERE tt.id = :ticketTypeId AND tt.availableQuantity >= :quantity")
  int reserve(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);

  @Modifying
  @Transactional
  @Query("UPDATE TicketTypeEntity tt SET tt.availableQuantity = tt.availableQuantity + :quantity WHERE tt.id = :ticketTypeId")
  int release(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);
}
