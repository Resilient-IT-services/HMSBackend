package com.res.hms.pharmacy.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pharmacy_stock_transaction", schema = "pharmacy")
public class PharmacyStockTransaction extends HospitalScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "medicine_id", nullable = false)
  private Medicine medicine;

  @Column(name = "batch_number", nullable = false)
  private String batchNumber;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "purchase_price", precision = 12, scale = 2)
  private BigDecimal purchasePrice = BigDecimal.ZERO;

  @Column(name = "selling_price", precision = 12, scale = 2)
  private BigDecimal sellingPrice = BigDecimal.ZERO;

  @Column(name = "taxable_amount", precision = 12, scale = 2)
  private BigDecimal taxableAmount = BigDecimal.ZERO;

  @Column(name = "tax_amount", precision = 12, scale = 2)
  private BigDecimal taxAmount = BigDecimal.ZERO;

  @Column(name = "net_amount", precision = 12, scale = 2)
  private BigDecimal netAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PharmacyStockTransactionType type;

  @Column(name = "transaction_date", nullable = false)
  private LocalDateTime transactionDate = LocalDateTime.now();

  private String reference;

  @Column(name = "supplier_name")
  private String supplierName;

  @Column(name = "supplier_state")
  private String supplierState;

  @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PharmacyStockTransactionTax> taxes = new ArrayList<>();
}
