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

  @Column(name = "test_id")
  private java.util.UUID testId;

  @Column(name = "test_name")
  private String testName;

  @Column(name = "package_id")
  private java.util.UUID packageId;

  @Column(name = "package_name")
  private String packageName;

  @Column(name = "notes")
  private String notes;

  @Column(name = "lab_order_test_id")
  private java.util.UUID labOrderTestId;

  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PrescriptionLabItemStatus status = PrescriptionLabItemStatus.PENDING;
}
