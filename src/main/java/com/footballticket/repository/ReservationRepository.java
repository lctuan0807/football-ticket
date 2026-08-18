package com.footballticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballticket.entity.ReservationEntity;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
  // List<ReservationEntity> findByStatusAndExpiresAtBefore(ReservationStatusEnum
  // status, LocalDateTime time);
}
