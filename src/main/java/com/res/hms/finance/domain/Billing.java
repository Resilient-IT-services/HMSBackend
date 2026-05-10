package com.res.hms.finance.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.res.hms.core.domain.HospitalScopedEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "billing")
public class Billing extends HospitalScopedEntity {

  @Column(name = "patient_id", nullable = false)
  private java.util.UUID patientId;

  @Column(name = "appointment_id")
  private java.util.UUID appointmentId;

  @Column(name = "doctor_id")
  private java.util.UUID doctorId;

  @Column(name = "doctor_name")
  private String doctorName;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false)
  private BillingSourceType sourceType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BillingStatus status;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(precision = 12, scale = 2)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(name = "payment_method")
  private String paymentMethod;

  @OneToMany(
      mappedBy = "billing",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<BillingItem> items = new ArrayList<>();
}
