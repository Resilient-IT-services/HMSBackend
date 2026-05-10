package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.DoctorAvailabilityException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorAvailabilityExceptionRepository
    extends JpaRepository<DoctorAvailabilityException, UUID> {

  List<DoctorAvailabilityException> findByDoctorIdAndHospitalId(UUID doctorId, UUID hospitalId);

  @Query(
      "SELECT e FROM DoctorAvailabilityException e WHERE e.doctorId = :doctorId AND e.hospitalId = :hospitalId AND e.date = :date")
  List<DoctorAvailabilityException> findByDoctorIdAndHospitalIdAndDate(
      @Param("doctorId") UUID doctorId,
      @Param("hospitalId") UUID hospitalId,
      @Param("date") LocalDate date);
}
