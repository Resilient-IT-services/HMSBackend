package com.res.hms.inventory.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
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

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "item_id", insertable = false, updatable = false)
  private Item item;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(name = "last_updated", nullable = false)
  private LocalDateTime lastUpdated;

  @jakarta.persistence.Version private Long version;
}
