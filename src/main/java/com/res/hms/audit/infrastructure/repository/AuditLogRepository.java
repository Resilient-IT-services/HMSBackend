package com.res.hms.audit.infrastructure.repository;

import com.res.hms.audit.domain.AuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  Page<AuditLog> findAllByHospitalIdOrderByCreatedAtDesc(UUID hospitalId, Pageable pageable);

  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
