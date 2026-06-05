package com.res.hms.lab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_price", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabPrice {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "hospital_id")
  private UUID hospitalId;

  @Column(name = "test_id")
  private UUID testId;

  @Column(name = "package_id")
  private UUID packageId;

  @Column(name = "standard_price")
  private BigDecimal standardPrice;

  @Column(name = "emergency_price")
  private BigDecimal emergencyPrice;

  @Column(name = "insurance_price")
  private BigDecimal insurancePrice;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;
}
