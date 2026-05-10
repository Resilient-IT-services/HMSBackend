package com.res.hms.iam.domain;

import java.util.UUID;

import com.res.hms.core.domain.AuditableEntity;
import com.res.hms.iam.application.dto.DoctorEducation;

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
@Table(name = "doctor_profile")
public class DoctorProfile extends AuditableEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @OneToOne(fetch = FetchType.EAGER)
  @MapsId
  @JoinColumn(name = "id")
  private UserAccount user;

  @Column(nullable = false)
  private String specialization;

  @Column(name = "license_number", unique = true, nullable = true)
  private String licenseNumber;

  @Column(columnDefinition = "text")
  private String bio;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "education_json", columnDefinition = "jsonb")
  private DoctorEducation education;

  public String getFullName() {
    return user != null ? user.getFullName() : null;
  }
}
