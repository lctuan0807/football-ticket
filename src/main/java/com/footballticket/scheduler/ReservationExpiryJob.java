package com.footballticket.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.footballticket.entity.ReservationEntity;
import com.footballticket.enums.ReservationStatusEnum;
import com.footballticket.repository.ReservationRepository;
import com.footballticket.repository.TicketTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "reservation.expiry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryJob {

  private final ReservationRepository reservationRepository;
  private final TicketTypeRepository ticketTypeRepository;

  @Value("${reservation.expiry.batch-size:200}")
  private int batchSize;

  @Scheduled(fixedDelayString = "${reservation.expiry.fixed-delay-ms:30000}",
      initialDelayString = "${reservation.expiry.initial-delay-ms:15000}")
  @Transactional
  public int expireStaleReservations() {
    Pageable batch = PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "expiresAt"));
    List<ReservationEntity> candidates = reservationRepository.findByStatusAndExpiresAtBefore(
        ReservationStatusEnum.PENDING.toInt(), LocalDateTime.now(), batch);

    if (candidates.isEmpty()) {
      return 0;
    }

    log.info("Found {} stale PENDING reservation(s) to expire", candidates.size());
    int expiredCount = 0;
    for (ReservationEntity reservation : candidates) {
      int updated = reservationRepository.updateStatusIfCurrentStatus(
          reservation.getId(), ReservationStatusEnum.PENDING.toInt(), ReservationStatusEnum.EXPIRED.toInt());
      if (updated == 0) {
        log.debug("Reservation {} already transitioned by a concurrent request; skipping release",
            reservation.getId());
        continue;
      }

      ticketTypeRepository.release(reservation.getTicketTypeId(), reservation.getQuantity());
      expiredCount++;
      log.info("Expired reservation id={} userId={} ticketTypeId={} quantity={}",
          reservation.getId(), reservation.getUserId(), reservation.getTicketTypeId(), reservation.getQuantity());
    }

    log.info("Reservation expiry sweep complete: {} of {} candidate(s) expired", expiredCount, candidates.size());
    if (candidates.size() == batchSize) {
      log.warn("Expiry batch fully saturated (batch-size={}); backlog may exist and will continue on next run",
          batchSize);
    }
    return expiredCount;
  }
}
