package com.footballticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI footballTicketOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Football Ticket API")
            .description("Ticket reservation backend for football matches")
            .version("v0.0.1"));
  }
}
