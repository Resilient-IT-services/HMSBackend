package com.res.hms.iam.domain;

import java.util.UUID;

import com.res.hms.core.domain.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lab_tech_profile")
public class LabTechProfile extends AuditableEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "id")
  private UserAccount user;

  private String specialty;

  @Column(name = "license_no")
  private String licenseNo;
}
