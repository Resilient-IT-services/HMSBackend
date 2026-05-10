package com.res.hms.audit.domain;

import java.util.UUID;

import org.hibernate.annotations.Type;

import com.res.hms.audit.application.dto.AuditDetail;
import com.res.hms.core.domain.BaseEntity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
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
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

  @Column(name = "hospital_id")
  private UUID hospitalId;

  @Column(name = "entity_name", nullable = false)
  private String entityName;

  @Column(name = "entity_id")
  private UUID entityId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuditAction action;

  @Column(name = "performed_by")
  private UUID performedBy;

  @Type(JsonType.class)
  @Column(name = "details", columnDefinition = "jsonb")
  private AuditDetail details;
}
