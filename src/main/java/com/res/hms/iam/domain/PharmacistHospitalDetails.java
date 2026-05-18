package com.res.hms.iam.domain;

import com.res.hms.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pharmacist_hospital_details")
public class PharmacistHospitalDetails extends AuditableEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "id")
  private UserContract assignment;

  @Column(name = "counter_location")
  private String counterLocation;

  @Column(name = "pharmacy_wing")
  private String pharmacyWing;

  @Column(name = "pharmacy_license_no")
  private String pharmacyLicenseNo;
}
