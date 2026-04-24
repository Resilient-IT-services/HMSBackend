package com.res.hms.repository;

import com.res.hms.domain.NurseHospitalDetails;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NurseHospitalDetailsRepository extends JpaRepository<NurseHospitalDetails, UUID> {}
