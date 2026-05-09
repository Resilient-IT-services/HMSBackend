package com.res.hms.clinical.application.patient.service;

import com.res.hms.clinical.application.patient.repository.PatientRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.application.patient.dto.PatientDTO;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {

  private final PatientRepository patientRepository;
  private final HospitalContext hospitalContext;
  private final com.res.hms.common.util.ResourceLookup resourceLookup;

  public PageResponse<PatientDTO> list(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (search != null && !search.trim().isEmpty()) {
      String searchPattern = "%" + search.trim().toLowerCase() + "%";
      return PageResponse.from(
          patientRepository.search(hospitalId, searchPattern, pageRequest).map(this::toDto));
    }

    return PageResponse.from(
        patientRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageRequest)
            .map(this::toDto));
  }

  public PatientDTO get(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return toDto(requirePatient(id, hospitalId));
  }

  public Patient requirePatient(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, patientRepository::findByIdAndHospitalId, "Patient");
  }

  public PatientDTO toDto(Patient patient) {
    return PatientDTO.builder()
        .id(patient.getId())
        .fullName(patient.getFullName())
        .patientCode(patient.getPatientCode())
        .email(patient.getEmail())
        .medicalHistory(patient.getMedicalHistory())
        .gender(patient.getGender())
        .dob(patient.getDob())
        .phone(patient.getPhone())
        .bloodGroup(patient.getBloodGroup())
        .address(patient.getAddress())
        .build();
  }
}
