package com.res.hms.iam.domain;

import com.res.hms.core.domain.AuditableEntity;
import com.res.hms.core.domain.CertificationLevel;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nurse_profile", schema = "staff")
public class NurseProfile extends AuditableEntity {

  @Id private UUID id;

  @OneToOne(fetch = FetchType.EAGER)
  @MapsId
  @JoinColumn(name = "id")
  private UserAccount user;

  @Column(name = "nursing_license_no", unique = true)
  private String nursingLicenseNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "certification_level")
  private CertificationLevel certificationLevel;

  public String getFullName() {
    return user != null ? user.getFullName() : null;
  }
}
