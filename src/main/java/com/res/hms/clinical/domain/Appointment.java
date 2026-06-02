package com.res.hms.clinical.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "appointment", schema = "appointment")
public class Appointment extends HospitalScopedEntity {

  @Column(name = "patient_id", nullable = false)
  private java.util.UUID patientId;

  @Column(name = "visit_id")
  private java.util.UUID visitId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinColumn(name = "visit_id", insertable = false, updatable = false)
  private PatientVisit visit;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinColumn(name = "patient_id", insertable = false, updatable = false)
  private Patient patient;

  @Column(name = "patient_name")
  private String patientName;

  @Column(name = "doctor_id", nullable = false)
  private java.util.UUID doctorId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinColumn(name = "doctor_id", insertable = false, updatable = false)
  private com.res.hms.iam.domain.DoctorProfile doctor;

  @Column(name = "doctor_name")
  private String doctorName;

  @Column(name = "appointment_time", nullable = false)
  private java.time.Instant appointmentTime;

  @Column(name = "appointment_date", nullable = false)
  private java.time.LocalDate appointmentDate;

  @Column(name = "appointment_slot_time", nullable = false)
  private java.time.LocalTime appointmentSlotTime;

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

  @jakarta.persistence.PrePersist
  @jakarta.persistence.PreUpdate
  private void populateDerivedAppointmentFields() {
    if (appointmentTime == null) {
      return;
    }

    java.time.OffsetDateTime utcAppointmentTime =
        appointmentTime.atOffset(java.time.ZoneOffset.UTC);
    if (appointmentDate == null) {
      appointmentDate = utcAppointmentTime.toLocalDate();
    }
    if (appointmentSlotTime == null) {
      appointmentSlotTime = utcAppointmentTime.toLocalTime();
    }
  }
}
