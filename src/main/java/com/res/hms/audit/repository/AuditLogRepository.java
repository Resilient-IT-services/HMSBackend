package com.res.hms.audit.repository;

import com.res.hms.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);
}
