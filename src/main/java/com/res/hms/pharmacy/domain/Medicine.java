package com.res.hms.pharmacy.domain;

import com.res.hms.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "medicine",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_medicine_name_strength_form",
          columnNames = {"name", "generic_name", "strength", "form"})
    },
    indexes = {
      @Index(name = "idx_medicine_name", columnList = "name"),
      @Index(name = "idx_medicine_generic_name", columnList = "generic_name"),
      @Index(name = "idx_medicine_composition", columnList = "composition")
    })
public class Medicine extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(name = "generic_name")
  private String genericName;

  private String composition;

  private String strength;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  private MedicineForm form;

  @Column(name = "schedule_type")
  @Enumerated(jakarta.persistence.EnumType.STRING)
  private MedicineSchedule scheduleType;

  private String manufacturer;

  @Column(nullable = false)
  private boolean deleted = false;

  @Column(name = "threshold_quantity", nullable = false)
  private Integer thresholdQuantity = 10;
}
