package com.res.hms.pharmacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hospital_medicine_catalog")
public class HospitalMedicineCatalog extends HospitalScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "medicine_id", nullable = false)
  private Medicine medicine;

  @Column(name = "reorder_level")
  private Integer reorderLevel = 0;

  @Column(name = "base_selling_price")
  private BigDecimal baseSellingPrice;

  @Column(name = "tax_percentage")
  private BigDecimal taxPercentage;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;
}
