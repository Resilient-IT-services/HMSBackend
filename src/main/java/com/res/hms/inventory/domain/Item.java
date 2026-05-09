package com.res.hms.inventory.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "item")
public class Item extends HospitalScopedEntity {

  @Column(nullable = false)
  private String name;

  private String category;

  private String unit;

  @Column(nullable = false)
  private boolean deleted = false;
}
