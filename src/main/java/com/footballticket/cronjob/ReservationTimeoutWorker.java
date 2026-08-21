package com.footballticket.cronjob;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.footballticket.config.KafkaTopicConfig;
import com.footballticket.entity.ReservationEntity;
import com.footballticket.enums.ReservationStatusEnum;
import com.footballticket.messaging.ReservationExpiredEvent;
import com.footballticket.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationTimeoutWorker {
  private static final int BATCH_SIZE = 100;

  private final ReservationRepository reservationRepository;
  private final KafkaTemplate<String, ReservationExpiredEvent> reservationExpiredKafkaTemplate;

  @Scheduled(fixedDelay = 3000)
  public void pollAndDispatch() {
    log.info("Processing reservations timeout");
    List<ReservationEntity> expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(
        ReservationStatusEnum.PENDING.toInt(), LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));
    log.info("Found {} reservations to process", expiredReservations.size());

    if (expiredReservations.isEmpty()) {
      return;
    }

    for (ReservationEntity reservation : expiredReservations) {
      Long id = reservation.getId();
      reservationExpiredKafkaTemplate.send(KafkaTopicConfig.RESERVATION_EXPIRED_TOPIC, String.valueOf(id),
          new ReservationExpiredEvent(id))
          .whenComplete((result, ex) -> {
            if (ex != null) {
              log.error("Failed to dispatch expiry for reservation {}", id, ex);
            } else {
              log.info("Dispatched expiry for reservation {}", id);
            }
          });
    }
  }
}
