package com.res.hms.tax.domain;

import com.res.hms.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "tax_group_detail", schema = "tax")
public class TaxGroupDetail extends AuditableEntity {

  @Id
  @UuidGenerator
  @Column(name = "id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private TaxGroup group;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tax_master_id", nullable = false)
  private TaxMaster taxMaster;
}
