package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pharmacy_stock")
public class PharmacyStock extends HospitalScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "medicine_id", nullable = false)
  private Medicine medicine;

  @Column(name = "batch_number", nullable = false)
  private String batchNumber;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false)
  private BigDecimal price;
}
