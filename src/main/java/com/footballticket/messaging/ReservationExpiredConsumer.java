package com.footballticket.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.footballticket.config.KafkaTopicConfig;
import com.footballticket.service.ReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationExpiredConsumer {

  private final ReservationService reservationService;

  @KafkaListener(topics = KafkaTopicConfig.RESERVATION_EXPIRED_TOPIC, groupId = "footballticket-reservation-expiry")
  public void consume(ReservationExpiredEvent event) {
    log.info("Received reservation expired event: {}", event);
    reservationService.expireReservation(event.reservationId());
  }
}
