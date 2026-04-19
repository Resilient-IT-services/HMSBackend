package com.res.hms.domain;

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
@Table(name = "billing_item")
public class BillingItem extends HospitalScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "billing_id", nullable = false)
  private Billing billing;

  @Column(name = "item_name", nullable = false)
  private String itemName;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false)
  private BigDecimal subtotal;
}
