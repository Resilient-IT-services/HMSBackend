package com.res.hms.iam.infrastructure.repository;

import com.res.hms.iam.domain.AdminHospitalDetails;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminHospitalDetailsRepository extends JpaRepository<AdminHospitalDetails, UUID> {}
