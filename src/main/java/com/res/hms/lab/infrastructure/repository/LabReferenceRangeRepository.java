package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabReferenceRange;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabReferenceRangeRepository
    extends JpaRepository<LabReferenceRange, UUID>, JpaSpecificationExecutor<LabReferenceRange> {}
