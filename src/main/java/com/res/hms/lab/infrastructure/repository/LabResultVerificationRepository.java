package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabResultVerification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabResultVerificationRepository extends JpaRepository<LabResultVerification, UUID>, JpaSpecificationExecutor<LabResultVerification> {
}
