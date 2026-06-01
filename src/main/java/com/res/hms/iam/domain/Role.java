package com.res.hms.iam.domain;

import com.res.hms.core.domain.BaseEntity;
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
@Table(name = "role", schema = "iam")
public class Role extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true)
  private RoleCode code;

  @Column(nullable = false)
  private String name;

  private String description;

  @Column(name = "is_system", nullable = false)
  private boolean system;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;
}
