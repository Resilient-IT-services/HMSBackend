package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vital_range_configs")
public class VitalRangeConfig extends HospitalScopedEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "vital_type", nullable = false)
  private VitalType vitalType;

  @Column(name = "min_value")
  private BigDecimal minValue;

  @Column(name = "max_value")
  private BigDecimal maxValue;

  private String unit;
}
