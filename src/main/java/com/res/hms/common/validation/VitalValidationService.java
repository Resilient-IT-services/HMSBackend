package com.res.hms.common.validation;

import com.res.hms.appointment.dto.VitalsDTO;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.VitalSetting;
import com.res.hms.repository.VitalSettingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VitalValidationService {

  private final VitalSettingRepository vitalSettingRepository;
  private final HospitalContext hospitalContext;

  public void validate(VitalsDTO vitals) {
    if (vitals == null) return;
    UUID hospitalId = hospitalContext.requireHospitalId();
    List<VitalSetting> settings = vitalSettingRepository.findAllByHospitalId(hospitalId);

    settings.forEach(s -> {
      switch (s.getVitalName().toUpperCase()) {
        case "HEART_RATE":
          validateRange(s, vitals.getHeartRate() != null ? new BigDecimal(vitals.getHeartRate()) : null);
          break;
        case "TEMPERATURE":
          validateTemperature(s, vitals.getTemperature(), vitals.getTemperatureUnit());
          break;
        case "WEIGHT":
          validateWeight(s, vitals.getWeight(), vitals.getWeightUnit());
          break;
        case "OXYGEN_LEVEL":
          validateRange(s, vitals.getOxygenLevel());
          break;
        case "SYSTOLIC":
          validateRange(s, vitals.getSystolic() != null ? new BigDecimal(vitals.getSystolic()) : null);
          break;
        case "DIASTOLIC":
          validateRange(s, vitals.getDiastolic() != null ? new BigDecimal(vitals.getDiastolic()) : null);
          break;
      }
    });
  }

  private void validateTemperature(VitalSetting s, BigDecimal value, com.res.hms.domain.TemperatureUnit unit) {
    if (value == null) return;
    BigDecimal valueToValidate = value;
    if (s.getUnit() != null && unit != null) {
      com.res.hms.domain.TemperatureUnit settingUnit = com.res.hms.domain.TemperatureUnit.valueOf(s.getUnit().toUpperCase());
      valueToValidate = convertTemperature(value, unit, settingUnit);
    }
    validateRange(s, valueToValidate);
  }

  private BigDecimal convertTemperature(BigDecimal value, com.res.hms.domain.TemperatureUnit from, com.res.hms.domain.TemperatureUnit to) {
    if (from == to) return value;
    if (from == com.res.hms.domain.TemperatureUnit.FAHRENHEIT && to == com.res.hms.domain.TemperatureUnit.CELSIUS) {
      return value.subtract(new BigDecimal("32")).multiply(new BigDecimal("5")).divide(new BigDecimal("9"), 2, java.math.RoundingMode.HALF_UP);
    }
    if (from == com.res.hms.domain.TemperatureUnit.CELSIUS && to == com.res.hms.domain.TemperatureUnit.FAHRENHEIT) {
      return value.multiply(new BigDecimal("9")).divide(new BigDecimal("5"), 2, java.math.RoundingMode.HALF_UP).add(new BigDecimal("32"));
    }
    return value;
  }

  private void validateWeight(VitalSetting s, BigDecimal value, com.res.hms.domain.WeightUnit unit) {
    if (value == null) return;
    BigDecimal valueToValidate = value;
    if (s.getUnit() != null && unit != null) {
      com.res.hms.domain.WeightUnit settingUnit = com.res.hms.domain.WeightUnit.valueOf(s.getUnit().toUpperCase());
      valueToValidate = convertWeight(value, unit, settingUnit);
    }
    validateRange(s, valueToValidate);
  }

  private BigDecimal convertWeight(BigDecimal value, com.res.hms.domain.WeightUnit from, com.res.hms.domain.WeightUnit to) {
    if (from == to) return value;
    BigDecimal rate = new BigDecimal("2.20462");
    if (from == com.res.hms.domain.WeightUnit.KG && to == com.res.hms.domain.WeightUnit.LB) {
      return value.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    if (from == com.res.hms.domain.WeightUnit.LB && to == com.res.hms.domain.WeightUnit.KG) {
      return value.divide(rate, 2, java.math.RoundingMode.HALF_UP);
    }
    return value;
  }

  private void validateRange(VitalSetting s, BigDecimal value) {
    if (value == null) return;
    if (s.getMinValue() != null && value.compareTo(s.getMinValue()) < 0) {
      throw new ApiException("VALIDATION_ERROR", s.getVitalName() + " is below minimum allowed value: " + s.getMinValue(), HttpStatus.BAD_REQUEST);
    }
    if (s.getMaxValue() != null && value.compareTo(s.getMaxValue()) > 0) {
      throw new ApiException("VALIDATION_ERROR", s.getVitalName() + " is above maximum allowed value: " + s.getMaxValue(), HttpStatus.BAD_REQUEST);
    }
  }
}
