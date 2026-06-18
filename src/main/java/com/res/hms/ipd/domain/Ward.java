package com.res.hms.ipd.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
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
@Table(name = "ward", schema = "ipd")
public class Ward extends HospitalScopedEntity {

  @Column(name = "ward_code", nullable = false)
  private String wardCode;

  @Column(name = "ward_name", nullable = false)
  private String wardName;

  @Enumerated(EnumType.STRING)
  @Column(name = "ward_type", nullable = false)
  private WardType wardType;

  @Column(name = "total_beds", nullable = false)
  private Integer totalBeds = 0;

  @Column(nullable = false)
  private boolean active = true;

  private String remarks;
}
