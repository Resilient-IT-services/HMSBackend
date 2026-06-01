package com.res.hms.finance.domain;

import com.res.hms.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment_allocation", schema = "billing")
public class PaymentAllocation extends BaseEntity {

  @Column(name = "payment_id", nullable = false)
  private UUID paymentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id", insertable = false, updatable = false)
  private Payment payment;

  @Column(name = "invoice_id", nullable = false)
  private UUID invoiceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", insertable = false, updatable = false)
  private Invoice invoice;

  @Column(name = "allocated_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal allocatedAmount;
}
