package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nurse")
public class Nurse extends BaseEntity {

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", unique = true)
  private UserAccount user;

  @Column(nullable = false)
  private String department;

  @Column(name = "nursing_license_no", unique = true)
  private String nursingLicenseNo;

  @Column(name = "certification_level")
  private String certificationLevel;

  public String getFullName() {
    return user != null ? user.getFullName() : null;
  }
}
