package com.footballticket.cronjob;

import java.util.Set;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.footballticket.config.KafkaTopicConfig;
import com.footballticket.messaging.ReservationExpiredEvent;
import com.footballticket.service.RedisService;
import com.footballticket.service.impl.ReservationServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationTimeoutWorker {
  private static final int BATCH_SIZE = 100;

  private final RedisService redisService;
  private final KafkaTemplate<String, ReservationExpiredEvent> reservationExpiredKafkaTemplate;

  @Scheduled(fixedDelay = 3000)
  public void pollAndDispatch() {
    log.info("Processing reservations timeout");
    Set<String> expiredIds = redisService.zRangeByScore(
        ReservationServiceImpl.RESERVATION_EXPIRY_ZSET_KEY, 0, System.currentTimeMillis(), BATCH_SIZE);
    log.info("Found {} reservations to process", expiredIds.size());

    if (expiredIds.isEmpty()) {
      return;
    }

    for (String idStr : expiredIds) {
      Long id = Long.valueOf(idStr);
      reservationExpiredKafkaTemplate.send(KafkaTopicConfig.RESERVATION_EXPIRED_TOPIC, idStr,
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
