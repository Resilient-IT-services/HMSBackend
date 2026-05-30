package com.res.hms.pharmacy.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "pharmacy_stock",
    schema = "pharmacy",
    indexes = {
      @Index(
          name = "idx_pharmacy_stock_hospital_medicine",
          columnList = "hospital_id, medicine_id"),
      @Index(
          name = "idx_pharmacy_stock_fefo",
          columnList = "hospital_id, medicine_id, expiry_date"),
      @Index(
          name = "idx_pharmacy_stock_batch",
          columnList = "hospital_id, medicine_id, batch_number")
    })
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

  @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal purchasePrice = BigDecimal.ZERO;

  @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal sellingPrice = BigDecimal.ZERO;

  @Version private long version;
}
