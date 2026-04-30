package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "medicine")
public class Medicine extends HospitalScopedEntity {

  @Column(nullable = false)
  private String name;

  @Column(name = "generic_name")
  private String genericName;

  private String composition;

  private String category;

  private String manufacturer;

  @Column(name = "reorder_level")
  private Integer reorderLevel;

  @Column(name = "selling_price")
  private BigDecimal sellingPrice;

  @Column(name = "purchase_price")
  private BigDecimal purchasePrice;

  @Column(name = "tax_percentage")
  private BigDecimal taxPercentage;

  @Column(name = "storage_temperature")
  private String storageTemperature;

  @Column(name = "storage_humidity")
  private String storageHumidity;

  @Column(name = "special_notes")
  private String specialNotes;
}
