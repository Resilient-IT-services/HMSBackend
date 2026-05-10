package com.res.hms.iam.domain;

import com.res.hms.core.domain.AuditableEntity;
import com.res.hms.finance.domain.CommissionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doctor_hospital_details")
public class DoctorHospitalDetails extends AuditableEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "id")
  private UserRoleAssignment assignment;

  @Column(name = "consultation_fee", nullable = false)
  private BigDecimal consultationFee = BigDecimal.ZERO;

  @Column(name = "follow_up_days", nullable = false)
  private Integer followUpDays = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "commission_type")
  private CommissionType commissionType;

  @Column(name = "commission_value")
  private BigDecimal commissionValue;

  @Column(name = "is_visiting_consultant")
  private boolean visitingConsultant = false;
}
