package com.res.hms.repository;

import com.res.hms.domain.DoctorHospitalDetails;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorHospitalDetailsRepository
    extends JpaRepository<DoctorHospitalDetails, UUID> {

  @Query(
      "SELECT d FROM DoctorHospitalDetails d WHERE d.assignment.user.id = :userId AND d.assignment.hospitalId = :hospitalId")
  Optional<DoctorHospitalDetails> findByUserIdAndHospitalId(
      @Param("userId") UUID userId, @Param("hospitalId") UUID hospitalId);
}
