package com.res.hms.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class HospitalScopedEntity extends BaseEntity {

  @Column(name = "hospital_id", nullable = false)
  private UUID hospitalId;
}
