package com.res.hms.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreConfig {

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
