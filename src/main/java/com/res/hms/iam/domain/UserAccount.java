package com.res.hms.iam.domain;

import com.res.hms.core.domain.BaseEntity;
import com.res.hms.core.domain.Gender;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_account", schema = "iam")
public class UserAccount extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String username;

  @Column(unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "full_name")
  private String fullName;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  private LocalDate dob;

  private String phone;

  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private Set<UserContract> contracts = new HashSet<>();

  @Column(name = "is_superuser", nullable = false)
  private boolean superuser;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private boolean deleted;

  @Column(name = "last_login_at")
  private java.time.Instant lastLoginAt;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private DoctorProfile doctorProfile;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private NurseProfile nurseProfile;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private PharmacistProfile pharmacistProfile;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private LabTechProfile labTechProfile;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private ReceptionistProfile receptionistProfile;

  public Set<RoleCode> getRoleCodes() {
    return contracts.stream().map(a -> a.getRole().getCode()).collect(Collectors.toSet());
  }
}
