package com.res.hms.patient.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Patient;
import com.res.hms.patient.dto.CreatePatientRequest;
import com.res.hms.patient.dto.PatientResponse;
import com.res.hms.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final HospitalContext hospitalContext;
    private final AuditLogService auditLogService;

    @Transactional
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

        auditLogService.record(hospitalId, "patient", patient.getId(), "CREATE", "Registered patient " + patient.getFullName());
        return toResponse(patient);
    }

    public PageResponse<PatientResponse> list(int page, int size) {
        Long hospitalId = hospitalContext.requireHospitalId();
        return PageResponse.from(patientRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
                .map(this::toResponse));
    }

    public PatientResponse get(Long id) {
        Long hospitalId = hospitalContext.requireHospitalId();
        return toResponse(patientRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Patient not found", HttpStatus.NOT_FOUND)));
    }

    public Patient requirePatient(Long id, Long hospitalId) {
        return patientRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Patient not found", HttpStatus.NOT_FOUND));
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getGender(),
                patient.getDob(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getCreatedAt()
        );
    }
}
