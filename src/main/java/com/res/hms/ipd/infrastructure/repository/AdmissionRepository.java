package com.res.hms.ipd.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.ipd.domain.Admission;
import com.res.hms.ipd.domain.AdmissionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface AdmissionRepository extends BaseRepository<Admission> {
  Optional<Admission> findByAdmissionNumberAndHospitalId(String admissionNumber, UUID hospitalId);

  List<Admission> findAllByPatientIdAndStatus(UUID patientId, AdmissionStatus status);

  Optional<Admission> findByBedIdAndStatus(UUID bedId, AdmissionStatus status);

  List<Admission> findAllByDoctorIdAndStatusAndHospitalIdOrderByAdmissionDateTimeDesc(
      UUID doctorId, AdmissionStatus status, UUID hospitalId);
}
