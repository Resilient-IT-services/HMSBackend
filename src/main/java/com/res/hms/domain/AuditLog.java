package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

  @Column(name = "hospital_id")
  private UUID hospitalId;

  @Column(name = "entity_name", nullable = false)
  private String entityName;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(nullable = false)
  private String action;

  @Column(name = "performed_by")
  private UUID performedBy;

  @Column(name = "details", length = 4000)
  private String details;
}
