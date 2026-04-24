package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doctor_hospital_details")
public class DoctorHospitalDetails {

  @Id
  private UUID assignmentId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "assignment_id")
  private UserRoleAssignment assignment;

  @Column(name = "consultation_fee", nullable = false)
  private BigDecimal consultationFee = BigDecimal.ZERO;

  @Column(name = "follow_up_days", nullable = false)
  private Integer followUpDays = 0;

  @Column(name = "commission_type")
  private String commissionType;

  @Column(name = "commission_value")
  private BigDecimal commissionValue;

  @Column(name = "is_visiting_consultant")
  private boolean visitingConsultant = false;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
