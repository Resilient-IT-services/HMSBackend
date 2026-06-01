package com.res.hms.finance.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "refund", schema = "billing")
public class Refund extends HospitalScopedEntity {

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

  @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal refundAmount;

  @Column(name = "reason", nullable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private RefundStatus status;

  @Column(name = "source_type")
  @Enumerated(jakarta.persistence.EnumType.STRING)
  private FinanceSourceType sourceType;

  @Column(name = "source_id")
  private UUID sourceId;

  @Column(name = "processed_at")
  private java.time.Instant processedAt;
}
