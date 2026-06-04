package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_reference_range", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReferenceRange {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "parameter_id")
  private UUID parameterId;

  @Column(name = "gender")
  private String gender;

  @Column(name = "min_age")
  private Integer minAge;

  @Column(name = "max_age")
  private Integer maxAge;

  @Column(name = "age_unit")
  private String ageUnit;

  @Column(name = "min_value")
  private BigDecimal minValue;

  @Column(name = "max_value")
  private BigDecimal maxValue;

  @Column(name = "critical_min")
  private BigDecimal criticalMin;

  @Column(name = "critical_max")
  private BigDecimal criticalMax;
}
