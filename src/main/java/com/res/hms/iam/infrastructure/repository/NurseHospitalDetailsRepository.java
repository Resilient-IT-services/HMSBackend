package com.res.hms.iam.infrastructure.repository;

import com.res.hms.iam.domain.NurseHospitalDetails;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NurseHospitalDetailsRepository extends JpaRepository<NurseHospitalDetails, UUID> {}
