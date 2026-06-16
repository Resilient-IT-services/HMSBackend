package com.res.hms.pharmacy.domain;

import com.res.hms.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "medicine",
    schema = "pharmacy",
    indexes = {
      @Index(name = "idx_medicine_name", columnList = "name"),
      @Index(name = "idx_medicine_generic_name", columnList = "generic_name"),
      @Index(name = "idx_medicine_composition", columnList = "composition"),
      @Index(name = "idx_medicine_item_code", columnList = "item_code"),
      @Index(name = "idx_medicine_dept_type", columnList = "department, item_type")
    })
public class Medicine extends BaseEntity {

  @Column(name = "item_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ItemType itemType = ItemType.MEDICINE;

  @Enumerated(EnumType.STRING)
  private DepartmentType department;

  @Column(nullable = false)
  private String name;

  @Column(name = "item_code")
  private String itemCode;

  private String category;

  @Column(name = "base_unit")
  private String baseUnit;

  @Column(name = "generic_name")
  private String genericName;

  private String composition;

  private String strength;

  @Enumerated(EnumType.STRING)
  private MedicineForm form;

  @Column(name = "schedule_type")
  @Enumerated(EnumType.STRING)
  private MedicineSchedule scheduleType;

  private String manufacturer;

  @Column(nullable = false)
  private boolean deleted = false;

  @Column(name = "threshold_quantity", nullable = false)
  private Integer thresholdQuantity = 10;

  @Column(name = "hsn_code")
  private String hsnCode;

  @Column(name = "storage_temperature")
  private String storageTemperature;

  @Column(name = "is_batch_tracked")
  private Boolean isBatchTracked = false;

  @Column(name = "is_expiry_tracked")
  private Boolean isExpiryTracked = false;
}
