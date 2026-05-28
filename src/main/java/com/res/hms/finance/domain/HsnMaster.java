package com.res.hms.finance.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hsn_master")
public class HsnMaster extends HospitalScopedEntity {

  @Column(name = "hsn_code", nullable = false)
  private String hsnCode;

  @Column(name = "description")
  private String description;

  @Column(name = "tax_percentage", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxPercentage;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;
}
