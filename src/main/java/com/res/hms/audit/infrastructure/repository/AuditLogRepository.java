package com.res.hms.audit.infrastructure.repository;

import com.res.hms.audit.domain.AuditLog;
import com.res.hms.core.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository extends BaseRepository<AuditLog> {
  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
