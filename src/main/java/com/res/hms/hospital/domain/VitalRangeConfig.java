package com.res.hms.hospital.domain;

import com.res.hms.core.domain.BaseEntity;
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
public class VitalRangeConfig extends com.res.hms.core.domain.BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "vital_type", nullable = false)
  private VitalType vitalType;

  @Column(name = "min_value")
  private BigDecimal minValue;

  @Column(name = "max_value")
  private BigDecimal maxValue;
}
