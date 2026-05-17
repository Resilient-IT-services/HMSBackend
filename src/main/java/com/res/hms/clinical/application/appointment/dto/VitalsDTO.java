package com.res.hms.clinical.application.appointment.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalsDTO {
  @Pattern(
      regexp = "^\\d{2,3}/\\d{2,3}$",
      message = "Blood pressure must be in format SYS/DIA (e.g., 120/80)")
  private String bloodPressure;

  private Integer systolic;
  private Integer diastolic;

  private Integer heartRate;

  private BigDecimal oxygenLevel;

  private BigDecimal temperature;

  @Positive(message = "Weight must be a positive value")
  private BigDecimal weight;
}
