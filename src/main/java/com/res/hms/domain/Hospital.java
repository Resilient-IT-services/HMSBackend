package com.res.hms.domain;

import com.res.hms.hospital.dto.HospitalSettings;
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
@Table(name = "hospital")
public class Hospital extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String code;

  private String address;

  private String city;

  private String state;

  private String pincode;

  @Column(unique = true)
  private String gstin;

  @Column(name = "registration_no")
  private String registrationNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HospitalStatus status;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "settings", columnDefinition = "jsonb")
  private HospitalSettings settings;
}
