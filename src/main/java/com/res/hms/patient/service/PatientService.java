package com.res.hms.patient.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Patient;
import com.res.hms.patient.dto.CreatePatientRequest;
import com.res.hms.patient.dto.PatientResponse;
import com.res.hms.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

  private final PatientRepository patientRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  @Auditable(entity = "patient", action = "CREATE")
  public PatientResponse create(CreatePatientRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Patient patient = new Patient();
    patient.setHospitalId(hospitalId);
    patient.setFullName(request.fullName());
    patient.setGender(request.gender());
    patient.setDob(request.dob());
    patient.setPhone(request.phone());
    patient.setAddress(request.address());
    patientRepository.save(patient);

    return toResponse(patient);
  }

  public PageResponse<PatientResponse> list(int page, int size, String search) {
    Long hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);
    
    if (search != null && !search.trim().isEmpty()) {
      return PageResponse.from(
          patientRepository
              .search(hospitalId, search.trim(), pageRequest)
              .map(this::toResponse));
    }

    return PageResponse.from(
        patientRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageRequest)
            .map(this::toResponse));
  }

  public PatientResponse get(Long id) {
    Long hospitalId = hospitalContext.requireHospitalId();
    return toResponse(requirePatient(id, hospitalId));
  }

  public Patient requirePatient(Long id, Long hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, patientRepository::findByIdAndHospitalId, "Patient");
  }

  private PatientResponse toResponse(Patient patient) {
    return new PatientResponse(
        patient.getId(),
        patient.getFullName(),
        patient.getGender(),
        patient.getDob(),
        patient.getPhone(),
        patient.getAddress(),
        patient.getCreatedAt());
  }
}
