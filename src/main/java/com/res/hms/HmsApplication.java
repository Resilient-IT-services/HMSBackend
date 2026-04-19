package com.res.hms;

import com.res.hms.config.AppBootstrapProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
@EnableConfigurationProperties(AppBootstrapProperties.class)
public class HmsApplication {

  protected HmsApplication() {
    // Utility class
  }

  public static void main(String[] args) {
    SpringApplication.run(HmsApplication.class, args);
  }
}
