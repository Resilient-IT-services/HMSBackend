package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabHospitalPackageConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabHospitalPackageConfigRepository
    extends JpaRepository<LabHospitalPackageConfig, UUID> {

  Optional<LabHospitalPackageConfig> findByHospitalIdAndPackageId(UUID hospitalId, UUID packageId);

  List<LabHospitalPackageConfig> findByHospitalId(UUID hospitalId);

  long countByHospitalIdAndActiveTrue(UUID hospitalId);
}
