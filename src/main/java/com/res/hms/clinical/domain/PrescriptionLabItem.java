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
@Table(name = "prescription_lab_item", schema = "clinical")
public class PrescriptionLabItem extends HospitalScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prescription_id", nullable = false)
  private Prescription prescription;

  @Column(name = "test_id", nullable = false)
  private java.util.UUID testId;

  @Column(name = "test_name", nullable = false)
  private String testName;

  @Column(name = "notes")
  private String notes;
}
