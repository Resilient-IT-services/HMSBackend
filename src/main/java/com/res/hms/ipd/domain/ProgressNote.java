package com.res.hms.ipd.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import com.res.hms.iam.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "progress_notes", schema = "ipd")
public class ProgressNote extends HospitalScopedEntity {

  @Column(name = "admission_id", nullable = false)
  private UUID admissionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admission_id", insertable = false, updatable = false)
  private Admission admission;

  @Column(name = "doctor_id", nullable = false)
  private UUID doctorId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", insertable = false, updatable = false)
  private UserAccount doctor;

  @Column(name = "note", nullable = false, columnDefinition = "TEXT")
  private String note;
}
