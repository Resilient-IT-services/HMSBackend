package com.res.hms.tax.domain;

import com.res.hms.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseTaxSnapshot extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @Column(name = "tax_code", nullable = false)
  private String taxCode;

  @Column(name = "tax_name", nullable = false)
  private String taxName;

  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxRate;

  @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal taxAmount;

  @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal taxableAmount;
}
