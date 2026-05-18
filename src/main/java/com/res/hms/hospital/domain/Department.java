package com.res.hms.hospital.domain;

import com.res.hms.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "department")
public class Department extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;
}
