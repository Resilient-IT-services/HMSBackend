package com.res.hms.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "billing")
public class Billing extends HospitalScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false)
  private BillingSourceType sourceType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BillingStatus status;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BillingItem> items = new ArrayList<>();
}
