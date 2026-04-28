package com.res.hms.prescription.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Prescription;
import java.util.List;
import java.util.UUID;

public interface PrescriptionRepository extends BaseRepository<Prescription> {
  List<Prescription> findByPatientIdAndHospitalIdOrderByCreatedAtDesc(UUID patientId, UUID hospitalId);
}
