package com.res.hms.tax.domain;

import com.res.hms.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "tax_master", schema = "tax")
public class TaxMaster extends AuditableEntity {

  @Id
  @UuidGenerator
  @Column(name = "id")
  private UUID id;

  @Column(name = "code", nullable = false, unique = true)
  private String code;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "tax_type", nullable = false)
  private String taxType;

  @Column(name = "rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal rate;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "effective_from", nullable = false)
  private Instant effectiveFrom;

  @Column(name = "effective_to")
  private Instant effectiveTo;
}
