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
@Table(name = "pharmacy_sale_item_tax", schema = "pharmacy")
public class PharmacySaleItemTax extends BaseTaxSnapshot {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pharmacy_sale_item_id", nullable = false)
  private PharmacySaleItem pharmacySaleItem;
}
