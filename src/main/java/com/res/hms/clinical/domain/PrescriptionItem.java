package com.res.hms.clinical.domain;

import com.res.hms.core.domain.HospitalScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prescription_item")
public class PrescriptionItem extends HospitalScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prescription_id", nullable = false)
  private Prescription prescription;

  @Column(name = "medicine_id", nullable = false)
  private java.util.UUID medicineId;

  @Column(name = "medicine_name")
  private String medicineName;

  @Column(nullable = false)
  private String dosage;

  @Column(nullable = false)
  private String duration;
}
