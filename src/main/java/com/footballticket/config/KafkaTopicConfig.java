package com.footballticket.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  public static final String RESERVATION_PLACE_TOPIC = "reservation-place-topic";

  @Bean
  public NewTopic reservationPlaceTopic() {
    return TopicBuilder.name(RESERVATION_PLACE_TOPIC)
        .partitions(1)
        .replicas(1)
        .build();
  }
}
