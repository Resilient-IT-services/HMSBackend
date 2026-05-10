package com.res.hms.clinical.domain;

import java.util.ArrayList;
import java.util.List;

import com.res.hms.core.domain.HospitalScopedEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prescription")
public class Prescription extends HospitalScopedEntity {

  @Column(name = "appointment_id", nullable = false)
  private java.util.UUID appointmentId;

  @Column(name = "patient_id", nullable = false)
  private java.util.UUID patientId;

  @Column(name = "patient_name")
  private String patientName;

  @Column(name = "doctor_id", nullable = false)
  private java.util.UUID doctorId;

  @Column(name = "doctor_name")
  private String doctorName;

  @Column(nullable = false)
  private boolean deleted = false;

  @OneToMany(
      mappedBy = "prescription",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<PrescriptionItem> items = new ArrayList<>();
}
