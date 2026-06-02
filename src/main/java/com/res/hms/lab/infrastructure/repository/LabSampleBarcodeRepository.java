package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabSampleBarcode;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabSampleBarcodeRepository extends JpaRepository<LabSampleBarcode, UUID>, JpaSpecificationExecutor<LabSampleBarcode> {
}
