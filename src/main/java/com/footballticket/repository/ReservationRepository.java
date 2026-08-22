package com.footballticket.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballticket.entity.ReservationEntity;

import jakarta.transaction.Transactional;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

  boolean existsByUserIdAndTicketTypeIdAndStatusAndExpiresAtAfter(
      Long userId, Long ticketTypeId, Integer status, LocalDateTime now);

  @Modifying
  @Transactional
  @Query("UPDATE reservations r SET r.status = :newStatus WHERE r.id = :id AND r.status = :expectedStatus")
  int updateStatusIfCurrentStatus(@Param("id") Long id, @Param("expectedStatus") Integer expectedStatus,
      @Param("newStatus") Integer newStatus);
}
