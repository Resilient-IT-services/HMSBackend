package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "patient")
public class Patient extends HospitalScopedEntity {

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "patient_code", unique = true)
  private String patientCode;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  private LocalDate dob;

  private String phone;

  private String address;
}
