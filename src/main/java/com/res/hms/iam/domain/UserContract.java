package com.res.hms.iam.domain;

import com.res.hms.core.domain.BaseEntity;
import com.res.hms.hospital.domain.Department;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_contract", schema = "iam")
public class UserContract extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "hospital_id")
  private UUID hospitalId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", insertable = false, updatable = false)
  private com.res.hms.hospital.domain.Hospital hospital;

  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "user_contract_department",
      schema = "iam",
      joinColumns = @JoinColumn(name = "user_contract_id"),
      inverseJoinColumns = @JoinColumn(name = "department_id"))
  private Set<Department> departments = new HashSet<>();

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @OneToOne(
      mappedBy = "assignment",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private DoctorHospitalDetails doctorDetails;

  @OneToOne(
      mappedBy = "assignment",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private NurseHospitalDetails nurseDetails;

  @OneToOne(
      mappedBy = "assignment",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private AdminHospitalDetails adminDetails;

  @OneToOne(
      mappedBy = "assignment",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private PharmacistHospitalDetails pharmacistDetails;

  @OneToOne(
      mappedBy = "assignment",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private ReceptionistHospitalDetails receptionistDetails;
}
