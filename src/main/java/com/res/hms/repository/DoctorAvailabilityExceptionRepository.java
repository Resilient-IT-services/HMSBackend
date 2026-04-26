package com.res.hms.repository;

import com.res.hms.domain.DoctorAvailabilityException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorAvailabilityExceptionRepository extends JpaRepository<DoctorAvailabilityException, UUID> {
  List<DoctorAvailabilityException> findByDoctorIdAndHospitalIdAndDate(
      UUID doctorId, UUID hospitalId, LocalDate date);
}
