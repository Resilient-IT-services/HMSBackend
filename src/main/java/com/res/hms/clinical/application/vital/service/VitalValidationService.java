package com.res.hms.clinical.application.vital.service;

import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.domain.VitalRangeConfig;
import com.res.hms.hospital.infrastructure.repository.VitalRangeConfigRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VitalValidationService {

  private final VitalRangeConfigRepository repository;

  public void validate(VitalsDTO vitals) {
    if (vitals == null) {
      return;
    }
    List<VitalRangeConfig> configs = repository.findAll();

    configs.forEach(
        c -> {
          switch (c.getVitalType()) {
            case HEART_RATE:
              validateRange(
                  c, vitals.getHeartRate() != null ? new BigDecimal(vitals.getHeartRate()) : null);
              break;
            case BODY_TEMPERATURE:
              validateRange(c, vitals.getTemperature());
              break;
            case WEIGHT:
              validateRange(c, vitals.getWeight());
              break;
            case OXYGEN_LEVEL:
              validateRange(c, vitals.getOxygenLevel());
              break;
            case SYSTOLIC_BP:
              validateRange(
                  c, vitals.getSystolic() != null ? new BigDecimal(vitals.getSystolic()) : null);
              break;
            case DIASTOLIC_BP:
              validateRange(
                  c, vitals.getDiastolic() != null ? new BigDecimal(vitals.getDiastolic()) : null);
              break;
            default:
              break;
          }
        });
  }

  private void validateRange(VitalRangeConfig c, BigDecimal value) {
    if (value == null) {
      return;
    }
    String vitalName = c.getVitalType().name().replace("_", " ").toLowerCase();
    // Capitalize first letter
    vitalName = vitalName.substring(0, 1).toUpperCase() + vitalName.substring(1);

    if (c.getMinValue() != null && value.compareTo(c.getMinValue()) < 0) {
      throw new ApiException(
          "VALIDATION_ERROR",
          vitalName + " must be between " + c.getMinValue() + " and " + c.getMaxValue(),
          HttpStatus.BAD_REQUEST);
    }
    if (c.getMaxValue() != null && value.compareTo(c.getMaxValue()) > 0) {
      throw new ApiException(
          "VALIDATION_ERROR",
          vitalName + " must be between " + c.getMinValue() + " and " + c.getMaxValue(),
          HttpStatus.BAD_REQUEST);
    }
  }
}
