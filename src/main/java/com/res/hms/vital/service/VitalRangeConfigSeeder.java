package com.res.hms.vital.service;

import com.res.hms.domain.VitalRangeConfig;
import com.res.hms.domain.VitalType;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.VitalRangeConfigRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VitalRangeConfigSeeder implements CommandLineRunner {

  private final HospitalRepository hospitalRepository;
  private final VitalRangeConfigRepository configRepository;

  @Override
  public void run(String... args) {
    seedDefaults();
  }

  @Transactional
  public void seedDefaults() {
    hospitalRepository
        .findAll()
        .forEach(
            hospital -> {
              ensureDefault(hospital.getId(), VitalType.BODY_TEMPERATURE, 95, 108, "F");
              ensureDefault(hospital.getId(), VitalType.SYSTOLIC_BP, 70, 220, "mmHg");
              ensureDefault(hospital.getId(), VitalType.DIASTOLIC_BP, 40, 140, "mmHg");
              ensureDefault(hospital.getId(), VitalType.OXYGEN_LEVEL, 50, 100, "%");
              ensureDefault(hospital.getId(), VitalType.WEIGHT, 1, 300, "kg");
              ensureDefault(hospital.getId(), VitalType.HEART_RATE, 30, 250, "bpm");
            });
  }

  private void ensureDefault(UUID hospitalId, VitalType type, double min, double max, String unit) {
    if (configRepository.findByHospitalIdAndVitalType(hospitalId, type).isEmpty()) {
      VitalRangeConfig config = new VitalRangeConfig();
      config.setHospitalId(hospitalId);
      config.setVitalType(type);
      config.setMinValue(BigDecimal.valueOf(min));
      config.setMaxValue(BigDecimal.valueOf(max));
      config.setUnit(unit);
      configRepository.save(config);
      log.info("Seeded default vital range for hospital {}: {}", hospitalId, type);
    }
  }
}
