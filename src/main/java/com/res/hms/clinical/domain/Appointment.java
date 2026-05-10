package com.res.hms.clinical.domain;

import java.time.LocalDateTime;

import com.res.hms.core.domain.HospitalScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "appointment")
public class Appointment extends HospitalScopedEntity {

  @Column(name = "patient_id", nullable = false)
  private java.util.UUID patientId;

  @Column(name = "patient_name")
  private String patientName;

  @Column(name = "doctor_id", nullable = false)
  private java.util.UUID doctorId;

  @Column(name = "doctor_name")
  private String doctorName;

  @Column(name = "appointment_time", nullable = false)
  private LocalDateTime appointmentTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AppointmentStatus status;

  @Column(name = "token_number")
  private Integer tokenNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "session_period")
  private SessionPeriod sessionPeriod;

  @Column(name = "chief_complaints")
  private String chiefComplaints;

  private String diagnosis;

  @Column(name = "clinical_notes")
  private String clinicalNotes;

  @Column(name = "icd_codes", columnDefinition = "jsonb")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  private java.util.List<String> icdCodes = new java.util.ArrayList<>();

  private String notes;

  @Column(nullable = false)
  private boolean deleted = false;

  @jakarta.persistence.OneToOne(
      mappedBy = "appointment",
      cascade = jakarta.persistence.CascadeType.ALL,
      fetch = jakarta.persistence.FetchType.LAZY)
  private Vitals vitals;
}
