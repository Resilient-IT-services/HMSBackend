package com.res.hms.repository;

import com.res.hms.domain.PharmacistHospitalDetails;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PharmacistHospitalDetailsRepository
    extends JpaRepository<PharmacistHospitalDetails, UUID> {}
