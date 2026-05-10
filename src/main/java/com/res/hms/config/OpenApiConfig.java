package com.res.hms.config;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.StringSchema;

@Configuration
public class OpenApiConfig {

  static {
    SpringDocUtils.getConfig()
        .replaceWithSchema(LocalTime.class, new StringSchema().example("09:00").pattern("HH:mm"));
    SpringDocUtils.getConfig()
        .replaceWithSchema(
            LocalDate.class, new StringSchema().example("2024-04-26").pattern("yyyy-MM-dd"));
  }

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI().components(new Components());
  }
}
