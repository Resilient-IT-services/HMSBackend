package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "billing")
public class Billing extends HospitalScopedEntity {

  @Column(name = "patient_id", nullable = false)
  private UUID patientId;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false)
  private BillingSourceType sourceType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BillingStatus status;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;
}
