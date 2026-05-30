package com.res.hms.tax.domain;

import com.res.hms.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "tax_rule", schema = "tax")
public class TaxRule extends AuditableEntity {

  @Id
  @UuidGenerator
  @Column(name = "id")
  private UUID id;

  @Column(name = "priority", nullable = false)
  private Integer priority = 0;

  @Column(name = "item_type", nullable = false)
  private String itemType;

  @Column(name = "hsn_code")
  private String hsnCode;

  @Column(name = "sac_code")
  private String sacCode;

  @Column(name = "patient_category")
  private String patientCategory;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tax_group_id", nullable = false)
  private TaxGroup taxGroup;

  @Column(name = "effective_from", nullable = false)
  private Instant effectiveFrom;

  @Column(name = "effective_to")
  private Instant effectiveTo;
}
