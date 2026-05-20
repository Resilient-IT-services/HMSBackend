package com.res.hms.clinical.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "patient_visit")
public class PatientVisit extends HospitalScopedEntity {

  @Column(name = "patient_id", nullable = false)
  private UUID patientId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", insertable = false, updatable = false)
  private Patient patient;

  @Column(name = "visit_number", nullable = false)
  private String visitNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "visit_type", nullable = false)
  private VisitType visitType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private VisitStatus status;

  @Column(name = "started_at", nullable = false)
  private java.time.Instant startedAt;

  @Column(name = "ended_at")
  private java.time.Instant endedAt;
}
