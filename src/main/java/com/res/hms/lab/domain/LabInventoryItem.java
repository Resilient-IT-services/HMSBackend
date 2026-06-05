package com.res.hms.lab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_inventory_item", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabInventoryItem {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "item_code")
  private String itemCode;

  @Column(name = "item_name")
  private String itemName;

  @Column(name = "unit")
  private String unit;
}
