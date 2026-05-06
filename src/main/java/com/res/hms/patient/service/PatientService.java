package com.res.hms.patient.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Patient;
import com.res.hms.patient.dto.PatientDTO;
import com.res.hms.patient.repository.PatientRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.res.hms.patient.mapper.PatientMapper;

@Service
@RequiredArgsConstructor
public class PatientService {

  private final PatientRepository patientRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;
  private final PatientMapper patientMapper;

  public PageResponse<PatientDTO> list(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (search != null && !search.trim().isEmpty()) {
      String searchPattern = "%" + search.trim().toLowerCase() + "%";
      return PageResponse.from(
          patientRepository.search(hospitalId, searchPattern, pageRequest).map(patientMapper::toDto));
    }

    return PageResponse.from(
        patientRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageRequest)
            .map(patientMapper::toDto));
  }

  public PatientDTO get(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return patientMapper.toDto(requirePatient(id, hospitalId));
  }

  public Patient requirePatient(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, patientRepository::findByIdAndHospitalId, "Patient");
  }

  public PatientDTO toDto(Patient patient) {
    return patientMapper.toDto(patient);
  }
}
