package com.res.hms.audit.infrastructure.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.res.hms.audit.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  Page<AuditLog> findAllByHospitalIdOrderByCreatedAtDesc(UUID hospitalId, Pageable pageable);

  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
