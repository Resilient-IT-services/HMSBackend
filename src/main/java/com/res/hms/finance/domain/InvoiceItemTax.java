package com.res.hms.finance.domain;

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
@Table(name = "invoice_item_tax", schema = "billing")
public class InvoiceItemTax extends BaseTaxSnapshot {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_item_id", nullable = false)
  private InvoiceItem invoiceItem;
}
