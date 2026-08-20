package com.footballticket.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.footballticket.enums.ReservationStatusEnum;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservationSchemaInitializer implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    jdbcTemplate.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_reservations_pending_user_ticket_type "
            + "ON reservations (user_id, ticket_type_id) WHERE status = "
            + ReservationStatusEnum.PENDING.toInt());
  }
}
