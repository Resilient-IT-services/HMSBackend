package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory_stock")
public class InventoryStock extends HospitalScopedEntity {

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(name = "last_updated", nullable = false)
  private LocalDateTime lastUpdated;
}
