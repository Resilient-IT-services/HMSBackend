package com.res.hms.ipd.domain;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.core.domain.HospitalScopedEntity;
import com.res.hms.iam.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admission", schema = "ipd")
public class Admission extends HospitalScopedEntity {

  @Column(name = "admission_number", nullable = false)
  private String admissionNumber;

  @Column(name = "patient_id", nullable = false)
  private UUID patientId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", insertable = false, updatable = false)
  private Patient patient;

  @Column(name = "doctor_id", nullable = false)
  private UUID doctorId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", insertable = false, updatable = false)
  private UserAccount doctor;

  @Column(name = "bed_id", nullable = false)
  private UUID bedId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bed_id", insertable = false, updatable = false)
  private Bed bed;

  @Column(name = "admission_date_time", nullable = false)
  private Instant admissionDateTime = Instant.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AdmissionStatus status = AdmissionStatus.ADMITTED;

  @Column(name = "admission_reason")
  private String admissionReason;

  private String remarks;
}
