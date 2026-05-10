package com.res.hms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.res.hms.config.AppBootstrapProperties;

@SpringBootApplication
@EnableMethodSecurity
@EnableConfigurationProperties(AppBootstrapProperties.class)
@SuppressWarnings("PMD.UseUtilityClass")
public class HmsApplication {

  public static void main(String[] args) {
    SpringApplication.run(HmsApplication.class, args);
  }
}
