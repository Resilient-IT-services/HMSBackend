package com.res.hms.clinical.application.service;

import com.res.hms.clinical.domain.PatientVisit;
import com.res.hms.clinical.domain.VisitStatus;
import com.res.hms.clinical.domain.VisitType;
import com.res.hms.clinical.infrastructure.repository.PatientVisitRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientVisitService {

  private final PatientVisitRepository patientVisitRepository;

  @Transactional
  public PatientVisit createVisit(UUID hospitalId, UUID patientId, VisitType type) {
    PatientVisit visit = new PatientVisit();
    visit.setHospitalId(hospitalId);
    visit.setPatientId(patientId);
    visit.setVisitType(type);
    visit.setStatus(VisitStatus.ACTIVE);
    visit.setStartedAt(java.time.Instant.now());
    visit.setVisitNumber("VST-" + System.currentTimeMillis());
    return patientVisitRepository.save(visit);
  }

  @Transactional
  public void closeVisit(UUID visitId) {
    patientVisitRepository
        .findById(visitId)
        .ifPresent(
            visit -> {
              visit.setStatus(VisitStatus.DISCHARGED);
              visit.setEndedAt(java.time.Instant.now());
              patientVisitRepository.save(visit);
            });
  }
}
