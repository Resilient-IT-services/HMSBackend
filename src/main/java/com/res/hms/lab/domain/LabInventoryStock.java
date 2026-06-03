package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_inventory_stock", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabInventoryStock {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "item_id")
  private UUID itemId;

  @Column(name = "hospital_id")
  private UUID hospitalId;

  @Column(name = "available_qty")
  private BigDecimal availableQty;

  @Column(name = "reorder_level")
  private BigDecimal reorderLevel;
}
