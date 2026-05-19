package com.res.hms.clinical.application.patient.service;

import com.res.hms.clinical.application.patient.dto.PatientDTO;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import jakarta.annotation.PostConstruct;
import java.util.List;
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

  @PostConstruct
  public void init() {
    try {
      backfillMissingPatientCodes();
    } catch (Exception e) {
      log.error("Failed to backfill missing patient codes during startup", e);
    }
  }

  @Transactional
  public PatientDTO savePatientAggregate(PatientDTO dto) {
    log.info("Starting aggregate save for patient: {}", dto.getFullName());
    UUID hospitalId = hospitalContext.requireHospitalId();

    String fullName = dto.getFullName() != null ? dto.getFullName().trim() : null;
    String phone = dto.getPhone() != null ? dto.getPhone().trim() : null;

    if (dto.getId() == null) {
      if (patientRepository.existsByFullNameAndPhoneAndHospitalId(fullName, phone, hospitalId)) {
        throw new ApiException(
            "ALREADY_EXISTS",
            "Patient with this name and phone number already exists",
            HttpStatus.CONFLICT);
      }
    } else {
      if (patientRepository.existsByFullNameAndPhoneAndHospitalIdAndIdNot(
          fullName, phone, hospitalId, dto.getId())) {
        throw new ApiException(
            "ALREADY_EXISTS",
            "Another patient with this name and phone number already exists",
            HttpStatus.CONFLICT);
      }
    }

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

  @Transactional
  public void backfillMissingPatientCodes() {
    log.info("Starting backfill for missing patient codes");
    List<Patient> patients = patientRepository.findAllByPatientCodeIsNullOrderByCreatedAtAsc();
    log.info("Found {} patients with missing codes", patients.size());

    for (Patient patient : patients) {
      String newCode = generatePatientCode(patient.getHospitalId(), patient.getCreatedAt());
      patient.setPatientCode(newCode);
      patientRepository.save(patient);
      log.info("Backfilled patient {} with code {}", patient.getId(), newCode);
    }
    patientRepository.flush();
  }

  private Patient createPatient(PatientDTO dto, UUID hospitalId) {
    Patient patient = new Patient();
    patient.setHospitalId(hospitalId);
    updatePatientFields(patient, dto);
    patient.setPatientCode(generatePatientCode(hospitalId, java.time.Instant.now()));
    return patient;
  }

  private String generatePatientCode(UUID hospitalId, java.time.Instant date) {
    java.time.ZonedDateTime zdt = date.atZone(java.time.ZoneOffset.UTC);
    String yearSuffix = String.valueOf(zdt.getYear()).substring(2);
    String prefix = yearSuffix + "-%";

    return patientRepository
        .findLastPatientCodeGlobal(prefix)
        .map(
            lastCode -> {
              try {
                String[] parts = lastCode.split("-");
                if (parts.length == 2) {
                  int lastNumber = Integer.parseInt(parts[1]);
                  return yearSuffix + "-" + String.format("%03d", lastNumber + 1);
                }
              } catch (NumberFormatException e) {
                log.warn("Failed to parse last patient code: {}", lastCode);
              }
              return yearSuffix + "-001";
            })
        .orElse(yearSuffix + "-001");
  }

  private void updatePatient(Patient patient, PatientDTO dto) {
    updatePatientFields(patient, dto);
  }

  private void updatePatientFields(Patient patient, PatientDTO dto) {
    patient.setFullName(dto.getFullName() != null ? dto.getFullName().trim() : null);
    patient.setGender(dto.getGender());
    patient.setDob(dto.getDob());
    patient.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
    patient.setEmail(dto.getEmail());
    patient.setMedicalHistory(dto.getMedicalHistory());
    patient.setBloodGroup(dto.getBloodGroup());
    patient.setAddress(dto.getAddress());
  }
}
