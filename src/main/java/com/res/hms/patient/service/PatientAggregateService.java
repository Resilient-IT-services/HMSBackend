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

import com.res.hms.patient.mapper.PatientMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientAggregateService {

  private final PatientRepository patientRepository;
  private final PatientService patientService;
  private final HospitalContext hospitalContext;
  private final PatientMapper patientMapper;

  @Transactional
  public PatientDTO savePatientAggregate(PatientDTO dto) {
    log.info("Starting aggregate save for patient: {}", dto.getFullName());
    UUID hospitalId = hospitalContext.requireHospitalId();

    Patient patient;
    if (dto.getId() == null) {
      patient = createPatient(dto, hospitalId);
    } else {
      patient = patientService.requirePatient(dto.getId(), hospitalId);
      updatePatient(patient, dto);
    }

    patientRepository.saveAndFlush(patient);
    return patientService.toDto(patient);
  }

  private Patient createPatient(PatientDTO dto, UUID hospitalId) {
    Patient patient = new Patient();
    patient.setHospitalId(hospitalId);
    patientMapper.updateEntity(dto, patient);
    return patient;
  }

  private void updatePatient(Patient patient, PatientDTO dto) {
    patientMapper.updateEntity(dto, patient);
  }
}
