package com.res.hms.patient.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Patient;
import com.res.hms.patient.dto.PatientDTO;
import com.res.hms.patient.repository.PatientRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientAggregateService {

  private final PatientRepository patientRepository;
  private final PatientService patientService;
  private final HospitalContext hospitalContext;

  @Transactional
  public PatientDTO savePatientAggregate(PatientDTO dto) {
    log.info("Starting aggregate save for patient: {}", dto.getFullName());
    UUID hospitalId = hospitalContext.requireHospitalId();

    Patient patient;
    if (dto.getId() == null) {
      patient = createPatient(dto, hospitalId);
    } else {
      patient =
          patientRepository
              .findByIdAndHospitalId(dto.getId(), hospitalId)
              .orElseThrow(
                  () -> new ApiException("NOT_FOUND", "Patient not found", HttpStatus.NOT_FOUND));
      updatePatient(patient, dto);
    }

    patientRepository.saveAndFlush(patient);
    return patientService.toDto(patient);
  }

  private Patient createPatient(PatientDTO dto, UUID hospitalId) {
    Patient patient = new Patient();
    patient.setHospitalId(hospitalId);
    updatePatientFields(patient, dto);
    return patient;
  }

  private void updatePatient(Patient patient, PatientDTO dto) {
    updatePatientFields(patient, dto);
  }

  private void updatePatientFields(Patient patient, PatientDTO dto) {
    patient.setFullName(dto.getFullName());
    patient.setGender(dto.getGender());
    patient.setDob(dto.getDob());
    patient.setPhone(dto.getPhone());
    patient.setEmail(dto.getEmail());
    patient.setMedicalHistory(dto.getMedicalHistory());
    patient.setBloodGroup(dto.getBloodGroup());
    patient.setAddress(dto.getAddress());
  }
}
