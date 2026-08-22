package com.footballticket.cronjob;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.footballticket.config.KafkaTopicConfig;
import com.footballticket.messaging.ReservationExpiredEvent;
import com.footballticket.service.RedisService;
import com.footballticket.service.impl.ReservationServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservationTimeoutWorkerTest {

  @Mock
  private RedisService redisService;

  @Mock
  private KafkaTemplate<String, ReservationExpiredEvent> reservationExpiredKafkaTemplate;

  private ReservationTimeoutWorker worker;

  @BeforeEach
  void setUp() {
    worker = new ReservationTimeoutWorker(redisService, reservationExpiredKafkaTemplate);
  }

  @Test
  void pollAndDispatch_sendsOneEventPerExpiredReservation() {
    given(redisService.zRangeByScore(eq(ReservationServiceImpl.RESERVATION_EXPIRY_ZSET_KEY), any(Double.class),
        any(Double.class), any(Long.class))).willReturn(Set.of("100", "200"));
    given(reservationExpiredKafkaTemplate.send(eq(KafkaTopicConfig.RESERVATION_EXPIRED_TOPIC), any(), any()))
        .willReturn(CompletableFuture.completedFuture(null));

    worker.pollAndDispatch();

    verify(reservationExpiredKafkaTemplate).send(KafkaTopicConfig.RESERVATION_EXPIRED_TOPIC, "100",
        new ReservationExpiredEvent(100L));
    verify(reservationExpiredKafkaTemplate).send(KafkaTopicConfig.RESERVATION_EXPIRED_TOPIC, "200",
        new ReservationExpiredEvent(200L));
  }

  @Test
  void pollAndDispatch_sendsNothing_whenNoExpiredReservations() {
    given(redisService.zRangeByScore(eq(ReservationServiceImpl.RESERVATION_EXPIRY_ZSET_KEY), any(Double.class),
        any(Double.class), any(Long.class))).willReturn(Set.of());

    worker.pollAndDispatch();

    verify(reservationExpiredKafkaTemplate, never()).send(any(), any(), any());
  }
}
