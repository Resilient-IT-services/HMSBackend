package com.res.hms.appointment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

  @Min(value = 30, message = "Heart rate must be at least 30 bpm")
  @Max(value = 250, message = "Heart rate must be at most 250 bpm")
  private Integer heartRate;

  @DecimalMin(value = "30.0", message = "Temperature must be at least 30.0")
  @DecimalMax(value = "45.0", message = "Temperature must be at most 45.0")
  private BigDecimal temperature;

  @Positive(message = "Weight must be a positive value")
  private BigDecimal weight;
}
