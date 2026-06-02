package com.res.hms.laboratory.domain;

import com.res.hms.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "laboratory_test", schema = "laboratory")
public class LaboratoryTest extends BaseEntity {

  @Column(name = "test_code", nullable = false)
  private String testCode;

  @Column(name = "test_name", nullable = false)
  private String testName;

  @Column(name = "short_name")
  private String shortName;

  @Column(name = "alias_name")
  private String aliasName;

  @Column(name = "active_status", nullable = false)
  private boolean active = true;
}
