package com.footballticket.config;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.footballticket.messaging.ReservationExpiredEvent;

@Configuration
public class KafkaProducerConfig {

  // Boot's autoconfigured KafkaTemplate is typed <Object, Object> and isn't
  // assignable to a KafkaTemplate<String, ReservationExpiredEvent> injection
  // point, so this app defines its own event-typed template.
  @Bean
  public ProducerFactory<String, ReservationExpiredEvent> reservationExpiredProducerFactory(
      KafkaProperties kafkaProperties) {
    return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(),
        null, new JsonSerializer<ReservationExpiredEvent>());
  }

  @Bean
  public KafkaTemplate<String, ReservationExpiredEvent> reservationExpiredKafkaTemplate(
      ProducerFactory<String, ReservationExpiredEvent> reservationExpiredProducerFactory) {
    return new KafkaTemplate<>(reservationExpiredProducerFactory);
  }
}
