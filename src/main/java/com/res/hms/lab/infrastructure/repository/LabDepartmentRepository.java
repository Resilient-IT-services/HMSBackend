package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabDepartment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabDepartmentRepository
    extends JpaRepository<LabDepartment, UUID>, JpaSpecificationExecutor<LabDepartment> {
  java.util.List<LabDepartment> findByHospitalIdOrHospitalIdIsNull(java.util.UUID hospitalId);
}
