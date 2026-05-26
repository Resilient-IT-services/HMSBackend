package com.res.hms.clinical.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prescription")
public class Prescription extends HospitalScopedEntity {

  @Column(name = "appointment_id", nullable = false)
  private java.util.UUID appointmentId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "appointment_id", insertable = false, updatable = false)
  private Appointment appointment;

  @Column(name = "patient_id", nullable = false)
  private java.util.UUID patientId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "patient_id", insertable = false, updatable = false)
  private Patient patient;

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
