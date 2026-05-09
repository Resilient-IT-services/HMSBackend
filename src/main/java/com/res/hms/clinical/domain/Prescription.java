package com.res.hms.clinical.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import com.res.hms.iam.domain.DoctorProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  @jakarta.persistence.OneToOne(fetch = FetchType.LAZY)
  @jakarta.persistence.MapsId
  @JoinColumn(name = "id")
  private Appointment appointment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "doctor_id", nullable = false)
  private DoctorProfile doctorProfile;

  @Column(nullable = false)
  private boolean deleted = false;

  @OneToMany(
      mappedBy = "prescription",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<PrescriptionItem> items = new ArrayList<>();
}
