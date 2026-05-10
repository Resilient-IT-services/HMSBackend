package com.res.hms.clinical.application.vital.service;

import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.hospital.domain.VitalRangeConfig;
import com.res.hms.hospital.infrastructure.repository.VitalRangeConfigRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VitalValidationService {

  private final VitalRangeConfigRepository repository;
  private final HospitalContext hospitalContext;

  public void validate(VitalsDTO vitals) {
    if (vitals == null) {
      return;
    }
    UUID hospitalId = hospitalContext.requireHospitalId();
    List<VitalRangeConfig> configs = repository.findAllByHospitalId(hospitalId);

    configs.forEach(
        c -> {
          switch (c.getVitalType()) {
            case HEART_RATE:
              validateRange(
                  c, vitals.getHeartRate() != null ? new BigDecimal(vitals.getHeartRate()) : null);
              break;
            case BODY_TEMPERATURE:
              validateTemperature(c, vitals.getTemperature(), vitals.getTemperatureUnit());
              break;
            case WEIGHT:
              validateWeight(c, vitals.getWeight(), vitals.getWeightUnit());
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

  private void validateTemperature(
      VitalRangeConfig c, BigDecimal value, com.res.hms.core.domain.TemperatureUnit unit) {
    if (value == null) {
      return;
    }
    BigDecimal valueToValidate = value;
    if (c.getUnit() != null && unit != null) {
      try {
        com.res.hms.core.domain.TemperatureUnit settingUnit =
            com.res.hms.core.domain.TemperatureUnit.valueOf(
                c.getUnit().toUpperCase(java.util.Locale.ROOT));
        valueToValidate = convertTemperature(value, unit, settingUnit);
      } catch (IllegalArgumentException e) {
        // If unit in config is not valid, skip conversion and use raw value
      }
    }
    validateRange(c, valueToValidate);
  }

  private BigDecimal convertTemperature(
      BigDecimal value,
      com.res.hms.core.domain.TemperatureUnit from,
      com.res.hms.core.domain.TemperatureUnit to) {
    if (from == to) {
      return value;
    }
    if (from == com.res.hms.core.domain.TemperatureUnit.FAHRENHEIT
        && to == com.res.hms.core.domain.TemperatureUnit.CELSIUS) {
      return value
          .subtract(new BigDecimal("32"))
          .multiply(new BigDecimal("5"))
          .divide(new BigDecimal("9"), 2, java.math.RoundingMode.HALF_UP);
    }
    if (from == com.res.hms.core.domain.TemperatureUnit.CELSIUS
        && to == com.res.hms.core.domain.TemperatureUnit.FAHRENHEIT) {
      return value
          .multiply(new BigDecimal("9"))
          .divide(new BigDecimal("5"), 2, java.math.RoundingMode.HALF_UP)
          .add(new BigDecimal("32"));
    }
    return value;
  }

  private void validateWeight(
      VitalRangeConfig c, BigDecimal value, com.res.hms.core.domain.WeightUnit unit) {
    if (value == null) {
      return;
    }
    BigDecimal valueToValidate = value;
    if (c.getUnit() != null && unit != null) {
      try {
        com.res.hms.core.domain.WeightUnit settingUnit =
            com.res.hms.core.domain.WeightUnit.valueOf(
                c.getUnit().toUpperCase(java.util.Locale.ROOT));
        valueToValidate = convertWeight(value, unit, settingUnit);
      } catch (IllegalArgumentException e) {
        // Skip conversion
      }
    }
    validateRange(c, valueToValidate);
  }

  private BigDecimal convertWeight(
      BigDecimal value,
      com.res.hms.core.domain.WeightUnit from,
      com.res.hms.core.domain.WeightUnit to) {
    if (from == to) {
      return value;
    }
    BigDecimal rate = new BigDecimal("2.20462");
    if (from == com.res.hms.core.domain.WeightUnit.KG
        && to == com.res.hms.core.domain.WeightUnit.LB) {
      return value.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    if (from == com.res.hms.core.domain.WeightUnit.LB
        && to == com.res.hms.core.domain.WeightUnit.KG) {
      return value.divide(rate, 2, java.math.RoundingMode.HALF_UP);
    }
    return value;
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
