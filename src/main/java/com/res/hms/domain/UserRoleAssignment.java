package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_role_assignment")
public class UserRoleAssignment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", insertable = false, updatable = false)
  private Hospital hospital;

  @Column(name = "hospital_id")
  private UUID hospitalId;

  @Column(name = "department_name")
  private String departmentName;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @OneToOne(mappedBy = "assignment", fetch = FetchType.LAZY)
  private DoctorHospitalDetails doctorDetails;

  @OneToOne(mappedBy = "assignment", fetch = FetchType.LAZY)
  private NurseHospitalDetails nurseDetails;

  @OneToOne(mappedBy = "assignment", fetch = FetchType.LAZY)
  private AdminHospitalDetails adminDetails;

  @OneToOne(mappedBy = "assignment", fetch = FetchType.LAZY)
  private PharmacistHospitalDetails pharmacistDetails;

  @OneToOne(mappedBy = "assignment", fetch = FetchType.LAZY)
  private ReceptionistHospitalDetails receptionistDetails;

  @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DoctorSchedule> schedules = new ArrayList<>();
}
