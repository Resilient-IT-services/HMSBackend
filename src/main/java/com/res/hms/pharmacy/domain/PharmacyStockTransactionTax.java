package com.res.hms.pharmacy.domain;

import com.res.hms.tax.domain.BaseTaxSnapshot;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pharmacy_stock_transaction_tax", schema = "pharmacy")
public class PharmacyStockTransactionTax extends BaseTaxSnapshot {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transaction_id", nullable = false)
  private PharmacyStockTransaction transaction;
}
