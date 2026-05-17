package com.res.hms.clinical.application.vital.service;

import com.res.hms.hospital.domain.VitalRangeConfig;
import com.res.hms.hospital.domain.VitalType;
import com.res.hms.hospital.infrastructure.repository.VitalRangeConfigRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VitalRangeConfigSeeder implements CommandLineRunner {

  private final VitalRangeConfigRepository configRepository;

  @Override
  public void run(String... args) {
    seedDefaults();
  }

  @Transactional
  public void seedDefaults() {
    ensureDefault(VitalType.BODY_TEMPERATURE, 35, 42);
    ensureDefault(VitalType.SYSTOLIC_BP, 70, 220);
    ensureDefault(VitalType.DIASTOLIC_BP, 40, 140);
    ensureDefault(VitalType.OXYGEN_LEVEL, 50, 100);
    ensureDefault(VitalType.WEIGHT, 1, 300);
    ensureDefault(VitalType.HEART_RATE, 30, 250);
  }

  private void ensureDefault(VitalType type, double min, double max) {
    if (configRepository.findByVitalType(type).isEmpty()) {
      VitalRangeConfig config = new VitalRangeConfig();
      config.setVitalType(type);
      config.setMinValue(BigDecimal.valueOf(min));
      config.setMaxValue(BigDecimal.valueOf(max));
      configRepository.save(config);
      log.info("Seeded default global vital range: {}", type);
    }
  }
}
