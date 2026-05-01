package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory_transaction")
public class InventoryTransaction extends HospitalScopedEntity {

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "item_id", insertable = false, updatable = false)
  private Item item;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InventoryTransactionType type;

  @Column(nullable = false)
  private BigDecimal quantity;

  private String reference;
}
