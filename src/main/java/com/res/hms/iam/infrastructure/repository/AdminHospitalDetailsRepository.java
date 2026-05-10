package com.res.hms.iam.infrastructure.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.res.hms.iam.domain.AdminHospitalDetails;

@Repository
public interface AdminHospitalDetailsRepository extends JpaRepository<AdminHospitalDetails, UUID> {}
