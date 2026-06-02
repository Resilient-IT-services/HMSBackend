package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabMachineMaintenance;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabMachineMaintenanceRepository extends JpaRepository<LabMachineMaintenance, UUID>, JpaSpecificationExecutor<LabMachineMaintenance> {
}
